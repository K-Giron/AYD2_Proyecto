package com.megastore.inventoryservice.stock;

import com.megastore.inventoryservice.article.Article;
import com.megastore.inventoryservice.article.ArticleNotFoundException;
import com.megastore.inventoryservice.article.ArticleRepository;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.logistics.ShipmentService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import com.megastore.inventoryservice.posstock.PosStockService;
import com.megastore.inventoryservice.warehouse.Warehouse;
import com.megastore.inventoryservice.warehouse.WarehouseRepository;
import com.megastore.inventoryservice.warehouse.WarehouseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Existencia y movimientos (entrada/salida con folio) del almacén central.
 * Es la única forma en que {@code Stock.quantityOnHand} cambia: nunca se
 * edita directo.
 */
@Service
public class StockService {

    private final ArticleRepository articleRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final FolioGenerator folioGenerator;
    private final ShipmentService shipmentService;
    private final PointOfSaleService pointOfSaleService;
    private final PosStockService posStockService;
    private final AuditLogService auditLogService;
    private final WarehouseRepository warehouseRepository;
    private final Clock clock;

    public StockService(ArticleRepository articleRepository, StockRepository stockRepository,
                         StockMovementRepository movementRepository, FolioGenerator folioGenerator,
                         ShipmentService shipmentService, PointOfSaleService pointOfSaleService,
                         PosStockService posStockService, AuditLogService auditLogService,
                         WarehouseRepository warehouseRepository, Clock clock) {
        this.articleRepository = articleRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.folioGenerator = folioGenerator;
        this.shipmentService = shipmentService;
        this.pointOfSaleService = pointOfSaleService;
        this.posStockService = posStockService;
        this.auditLogService = auditLogService;
        this.warehouseRepository = warehouseRepository;
        this.clock = clock;
    }

    /** Resuelve el único almacén de origen de artículos/existencias/movimientos. */
    private Warehouse centralWarehouse() {
        return warehouseRepository.findByCode(Warehouse.CENTRAL_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el almacén central (" + Warehouse.CENTRAL_CODE + ")."));
    }

    /** Crea la fila de existencia en cero para un artículo recién dado de alta. */
    @Transactional
    public void initializeStock(UUID articleId) {
        stockRepository.save(Stock.initialize(articleId, centralWarehouse().getId(), clock.instant()));
    }

    @Transactional
    public StockMovement registerMovement(UUID articleId, StockMovementType type, int quantity,
                                           String reason, UUID pointOfSaleId, String externalReference) {
        if (externalReference != null) {
            // Reintento de quien llama (ej. una integración): el efecto ya ocurrió, se devuelve
            // el mismo movimiento en vez de descontar/sumar existencia una segunda vez.
            Optional<StockMovement> existing = movementRepository.findByExternalReference(externalReference);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (!articleRepository.existsById(articleId)) {
            throw new ArticleNotFoundException(articleId);
        }
        if (type == StockMovementType.SALIDA && pointOfSaleId == null) {
            throw new PointOfSaleRequiredException();
        }
        if (type == StockMovementType.AJUSTE) {
            if (reason == null || reason.isBlank()) {
                throw new AdjustmentReasonRequiredException();
            }
            pointOfSaleId = null;
        }
        if (pointOfSaleId != null) {
            // Valida existencia antes de tocar la existencia del artículo.
            pointOfSaleService.getResponseById(pointOfSaleId);
        }

        Instant now = clock.instant();
        int updatedRows = type == StockMovementType.ENTRADA
                ? stockRepository.increase(articleId, quantity, now)
                : stockRepository.decrease(articleId, quantity, now);
        if (updatedRows == 0) {
            throw new InsufficientStockException(articleId, quantity);
        }

        String folio = folioGenerator.next(type);
        // saveAndFlush (no solo save): esta fila se crea justo después de la
        // actualización masiva de arriba (@Modifying(clearAutomatically = true)),
        // y sin forzar el flush aquí la fila no queda escrita al confirmar la
        // transacción — se detectó al agregar la columna external_reference y
        // ver folios "fantasma" (respuesta 201 pero sin fila real en la BD).
        StockMovement movement = movementRepository.saveAndFlush(
                StockMovement.register(folio, type, articleId, centralWarehouse().getId(), quantity, reason,
                        pointOfSaleId, externalReference, now));

        if (type == StockMovementType.SALIDA) {
            // Cada folio de salida se convierte en un envío a su punto de venta destino (módulo de logística)
            // y, del otro lado, en una entrada que sube la existencia propia de ese punto de venta.
            shipmentService.createForFolio(folio, movement.getId(), pointOfSaleId);
            posStockService.receiveFromWarehouse(pointOfSaleId, articleId, quantity, folio);
        }

        auditLogService.record(
                switch (type) {
                    case ENTRADA -> "ENTRADA_REGISTRADA";
                    case SALIDA -> "SALIDA_REGISTRADA";
                    case AJUSTE -> "AJUSTE_REGISTRADO";
                },
                (switch (type) {
                    case ENTRADA -> "Entrada de ";
                    case SALIDA -> "Salida de ";
                    case AJUSTE -> "Ajuste (baja) de ";
                }) + quantity
                        + " unidad(es) registrada en el almacén central con folio " + folio
                        + (reason == null || reason.isBlank() ? "." : " — " + reason + "."),
                folio, pointOfSaleId);
        return movement;
    }

    /** Resuelve el punto de venta de un movimiento para mostrarlo en la respuesta (null si es entrada). */
    @Transactional(readOnly = true)
    public PointOfSaleResponse resolvePointOfSale(UUID pointOfSaleId) {
        return pointOfSaleId == null ? null : pointOfSaleService.getResponseById(pointOfSaleId);
    }

    /** Resuelve sku/nombre del artículo de un movimiento para mostrarlo en la respuesta. */
    @Transactional(readOnly = true)
    public ArticleSummary resolveArticle(UUID articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
        return new ArticleSummary(article.getSku(), article.getName());
    }

    /** Resuelve el almacén de origen de un movimiento para mostrarlo en la respuesta. */
    @Transactional(readOnly = true)
    public WarehouseResponse resolveWarehouse(UUID warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .map(WarehouseResponse::from)
                .orElseThrow(() -> new IllegalStateException("No existe el almacén con id " + warehouseId));
    }

    @Transactional(readOnly = true)
    public int getQuantityOnHand(UUID articleId) {
        return stockRepository.findByArticleId(articleId)
                .map(Stock::getQuantityOnHand)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
    }

    @Transactional(readOnly = true)
    public List<StockBalance> listStock() {
        List<Article> articles = articleRepository.findAllByOrderByNameAsc();
        return articles.stream()
                .map(article -> {
                    Stock stock = stockRepository.findByArticleId(article.getId())
                            .orElseThrow(() -> new ArticleNotFoundException(article.getId()));
                    return new StockBalance(article, stock, resolveWarehouse(stock.getWarehouseId()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovement> listMovements(UUID articleId) {
        return articleId != null
                ? movementRepository.findByArticleIdOrderByOccurredAtDesc(articleId)
                : movementRepository.findAllByOrderByOccurredAtDesc();
    }

    /** Artículo, existencia y almacén de origen para las respuestas de consulta de stock. */
    public record StockBalance(Article article, Stock stock, WarehouseResponse warehouse) {
    }
}
