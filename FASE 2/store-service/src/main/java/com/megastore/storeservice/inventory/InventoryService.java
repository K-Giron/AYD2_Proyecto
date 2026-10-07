package com.megastore.storeservice.inventory;

import com.megastore.storeservice.shared.StoreBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Almacén de la tienda. Su catálogo es exactamente lo que el central le ha
 * enviado por folio; la existencia sube cuando la tienda confirma la
 * recepción de un folio y baja con ventas y traslados.
 */
@Service
public class InventoryService {

    private final StoreArticleRepository articleRepository;
    private final StoreStockRepository stockRepository;
    private final StoreReceiptRepository receiptRepository;
    private final Clock clock;

    public InventoryService(StoreArticleRepository articleRepository, StoreStockRepository stockRepository,
                            StoreReceiptRepository receiptRepository, Clock clock) {
        this.articleRepository = articleRepository;
        this.stockRepository = stockRepository;
        this.receiptRepository = receiptRepository;
        this.clock = clock;
    }

    /**
     * Registra una recepción que el central dice haber enviado, una sola vez
     * por folio, como EN_TRANSITO: todavía no suma a la existencia. Devuelve
     * false si ya se conocía (el central reenvía la última en cada consulta,
     * a propósito, para no perder ninguna).
     */
    @Transactional
    public boolean registerIncoming(IncomingReceipt incoming) {
        if (receiptRepository.existsById(incoming.folio())) {
            return false;
        }
        receiptRepository.saveAndFlush(StoreReceipt.inTransit(incoming));
        return true;
    }

    /** Lo que trae un folio de envío (SAL-… o TRA-…), recibido o no. */
    @Transactional(readOnly = true)
    public List<StoreReceipt> findShipment(String folio) {
        return receiptRepository.findByShipmentFolio(normalizeFolio(folio));
    }

    /**
     * La tienda confirma que llegó la mercadería de un folio: cada línea en
     * tránsito suma a la existencia (y da de alta el artículo si la tienda
     * aún no lo tenía). Todo o nada.
     */
    @Transactional
    public List<StoreReceipt> confirmShipment(String folio, String seller) {
        List<StoreReceipt> lines = receiptRepository.findByShipmentFolio(normalizeFolio(folio));
        if (lines.isEmpty()) {
            throw new StoreBusinessException(HttpStatus.NOT_FOUND, "FOLIO_NOT_FOUND",
                    "El folio " + folio + " no tiene mercadería enviada a esta tienda (o el central aún no lo ha entregado).");
        }
        List<StoreReceipt> pending = lines.stream()
                .filter(line -> line.getStatus() == StoreReceipt.Status.EN_TRANSITO)
                .toList();
        if (pending.isEmpty()) {
            throw new StoreBusinessException(HttpStatus.CONFLICT, "FOLIO_ALREADY_RECEIVED",
                    "El folio " + folio + " ya fue recibido por " + lines.getFirst().getReceivedBy() + ".");
        }
        Instant now = clock.instant();
        List<StoreReceipt> received = new java.util.ArrayList<>();
        for (StoreReceipt line : pending) {
            if (receiptRepository.markReceived(line.getFolio(), seller, now) == 0) {
                continue;
            }
            Optional<StoreArticle> existing = articleRepository.findById(line.getArticleId());
            if (existing.isPresent()) {
                existing.get().refresh(line.getSku(), line.getName(), line.getDescription(), line.getPrice(), now);
            } else {
                // saveAndFlush: ids asignados a mano, seguidos de una actualización masiva
                // con clearAutomatically (la lección de los folios "fantasma" del central).
                articleRepository.saveAndFlush(StoreArticle.fromCentral(line.getArticleId(), line.getSku(),
                        line.getName(), line.getDescription(), line.getPrice(), now));
            }
            if (!stockRepository.existsById(line.getArticleId())) {
                stockRepository.saveAndFlush(StoreStock.empty(line.getArticleId(), now));
            }
            stockRepository.increase(line.getArticleId(), line.getQuantity(), now);
            line.markReceived(seller, now);
            received.add(line);
        }
        if (received.isEmpty()) {
            throw new StoreBusinessException(HttpStatus.CONFLICT, "FOLIO_ALREADY_RECEIVED",
                    "El folio " + folio + " acaba de ser recibido por otra persona.");
        }
        return received;
    }

    private static String normalizeFolio(String folio) {
        return folio == null ? "" : folio.trim().toUpperCase(java.util.Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public long countShipmentsInTransit() {
        return receiptRepository.findByStatusOrderByOccurredAtAsc(StoreReceipt.Status.EN_TRANSITO).stream()
                .map(StoreReceipt::getShipmentFolio)
                .distinct()
                .count();
    }

    @Transactional(readOnly = true)
    public Optional<Instant> latestReceiptAt() {
        return receiptRepository.findLatestOccurredAt();
    }

    /** Descuenta dentro de la transacción de quien llama (venta o traslado): si no alcanza, todo se revierte. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void take(StoreArticle article, int quantity) {
        if (stockRepository.decrease(article.getId(), quantity, clock.instant()) == 0) {
            throw new StoreBusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "No hay existencia suficiente de " + article.getName() + " (" + article.getSku()
                            + ") para " + quantity + " unidad(es); disponible: " + quantityOf(article.getId()) + ".");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void giveBack(UUID articleId, int quantity) {
        stockRepository.increase(articleId, quantity, clock.instant());
    }

    @Transactional(readOnly = true)
    public StoreArticle getArticle(UUID articleId) {
        return articleRepository.findById(articleId).orElseThrow(() -> new StoreBusinessException(
                HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "Esta tienda no tiene el artículo " + articleId + "."));
    }

    @Transactional(readOnly = true)
    public StockItemResponse findBySku(String sku) {
        StoreArticle article = articleRepository.findBySkuIgnoreCase(sku.trim()).orElseThrow(() -> new StoreBusinessException(
                HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND",
                "El código " + sku + " no está en el almacén de esta tienda. Solo se vende lo que el central envió por folio."));
        return toResponse(article, stockRepository.findById(article.getId()).orElse(null), 0);
    }

    /** Hasta 30 artículos cuyo código, nombre o descripción contenga el texto (sin distinguir mayúsculas). */
    @Transactional(readOnly = true)
    public List<StockItemResponse> search(String text) {
        String trimmed = text == null ? "" : text.trim().toLowerCase(java.util.Locale.ROOT);
        if (trimmed.length() < 2) {
            return List.of();
        }
        String pattern = "%" + trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return articleRepository.search(pattern, org.springframework.data.domain.Limit.of(30)).stream()
                .map(article -> toResponse(article, stockRepository.findById(article.getId()).orElse(null), 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<StoreArticle> findByName(String name) {
        List<StoreArticle> matches = articleRepository.findByNameIgnoreCase(name.trim());
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    /** Unidades de un artículo que el central ya envió y la tienda aún no confirma (folios en tránsito). */
    @Transactional(readOnly = true)
    public int unitsInTransit(UUID articleId) {
        return receiptRepository.findByStatusOrderByOccurredAtAsc(StoreReceipt.Status.EN_TRANSITO).stream()
                .filter(receipt -> articleId.equals(receipt.getArticleId()))
                .mapToInt(StoreReceipt::getQuantity).sum();
    }

    @Transactional(readOnly = true)
    public int quantityOf(UUID articleId) {
        return stockRepository.findById(articleId).map(StoreStock::getQuantity).orElse(0);
    }

    @Transactional(readOnly = true)
    public List<StockItemResponse> listStock() {
        Map<UUID, StoreStock> stock = stockRepository.findAll().stream()
                .collect(Collectors.toMap(StoreStock::getArticleId, Function.identity()));
        Map<UUID, Integer> inTransit = receiptRepository.findByStatusOrderByOccurredAtAsc(StoreReceipt.Status.EN_TRANSITO)
                .stream()
                .collect(Collectors.toMap(StoreReceipt::getArticleId, StoreReceipt::getQuantity, Integer::sum));
        return articleRepository.findAllByOrderByNameAsc().stream()
                .map(article -> toResponse(article, stock.get(article.getId()), inTransit.getOrDefault(article.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreReceipt> listReceipts() {
        return receiptRepository.findTop200ByOrderByOccurredAtDesc();
    }

    private StockItemResponse toResponse(StoreArticle article, StoreStock stock, int inTransit) {
        return new StockItemResponse(article.getId(), article.getSku(), article.getName(), article.getDescription(),
                article.getPrice(),
                stock == null ? 0 : stock.getQuantity(), inTransit,
                stock == null ? article.getUpdatedAt() : stock.getUpdatedAt());
    }
}
