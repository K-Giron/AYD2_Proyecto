package com.megastore.inventoryservice.posstock;

import com.megastore.inventoryservice.posevents.PosStockEventService;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Existencia propia de cada punto de venta, alimentada únicamente por los
 * folios de salida que registra el almacén central (nunca se captura a
 * mano). Es el lado "punto de venta" del mismo folio: el almacén lo ve
 * como salida, el punto de venta lo ve como entrada.
 */
@Service
public class PosStockService {

    private final PosStockRepository stockRepository;
    private final PosStockMovementRepository movementRepository;
    private final PosFolioSequenceRepository folioSequenceRepository;
    private final AuditLogService auditLogService;
    private final PosStockEventService stockEvents;
    private final Clock clock;

    public PosStockService(PosStockRepository stockRepository, PosStockMovementRepository movementRepository,
                            PosFolioSequenceRepository folioSequenceRepository, AuditLogService auditLogService,
                            PosStockEventService stockEvents, Clock clock) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.folioSequenceRepository = folioSequenceRepository;
        this.auditLogService = auditLogService;
        this.stockEvents = stockEvents;
        this.clock = clock;
    }

    /** Llamado por {@code StockService} cuando el almacén registra una salida hacia un punto de venta. */
    @Transactional
    public PosStockMovement receiveFromWarehouse(UUID pointOfSaleId, UUID articleId, int quantity,
                                                  String originWarehouseFolio) {
        Instant now = clock.instant();
        ensureStockRow(pointOfSaleId, articleId, now);
        stockRepository.increase(pointOfSaleId, articleId, quantity, now);

        String folio = "POS-ENT-" + String.format("%06d", folioSequenceRepository.nextEntradaSequence());
        PosStockMovement movement = movementRepository.saveAndFlush(
                PosStockMovement.register(folio, pointOfSaleId, articleId, quantity, originWarehouseFolio, now));

        auditLogService.record("POS_ENTRADA_REGISTRADA",
                "El punto de venta recibió " + quantity + " unidad(es) del folio " + originWarehouseFolio
                        + " del almacén, con su propio folio " + folio + ".",
                originWarehouseFolio, pointOfSaleId);
        stockEvents.enqueue(pointOfSaleId, articleId, "ENTRADA", quantity, onHand(pointOfSaleId, articleId),
                folio, originWarehouseFolio);
        return movement;
    }

    /**
     * Venta en la tienda física: descuenta la existencia propia del punto
     * de venta. Idempotente por {@code externalReference} (un reintento de
     * red devuelve la misma venta). Sin existencia suficiente no se
     * registra nada.
     */
    @Transactional
    public PosStockMovement registerSale(UUID pointOfSaleId, UUID articleId, int quantity, String reason,
                                         String externalReference) {
        return tryRegisterSale(pointOfSaleId, articleId, quantity, reason, externalReference)
                .orElseThrow(() -> new InsufficientPosStockException(pointOfSaleId, articleId, quantity));
    }

    /**
     * Igual que {@link #registerSale}, pero sin existencia suficiente
     * devuelve vacío en vez de lanzar: la usa quien recibe una venta que ya
     * ocurrió (factura de tienda) y no debe abortar su transacción por eso.
     */
    @Transactional
    public Optional<PosStockMovement> tryRegisterSale(UUID pointOfSaleId, UUID articleId, int quantity, String reason,
                                                      String externalReference) {
        if (externalReference != null) {
            Optional<PosStockMovement> existing = movementRepository.findByExternalReference(externalReference);
            if (existing.isPresent()) {
                return existing;
            }
        }
        Instant now = clock.instant();
        if (stockRepository.decrease(pointOfSaleId, articleId, quantity, now) == 0) {
            return Optional.empty();
        }
        String folio = "POS-VTA-" + String.format("%06d", folioSequenceRepository.nextVentaSequence());
        // saveAndFlush: la fila se crea justo después de una actualización masiva con
        // clearAutomatically, el mismo caso que dejaba folios "fantasma" en StockService.
        PosStockMovement movement = movementRepository.saveAndFlush(
                PosStockMovement.sale(folio, pointOfSaleId, articleId, quantity, externalReference, now));
        auditLogService.record("POS_VENTA_REGISTRADA",
                "Venta en tienda de " + quantity + " unidad(es) con folio " + folio
                        + (reason == null || reason.isBlank() ? "." : " (" + reason + ")."),
                folio, pointOfSaleId);
        stockEvents.enqueue(pointOfSaleId, articleId, "VENTA", -quantity, onHand(pointOfSaleId, articleId),
                folio, externalReference);
        return Optional.of(movement);
    }

    /**
     * Salida de la tienda por traslado (hacia el central u otra tienda),
     * con el folio de traslado del central. Mismo descuento atómico que la
     * venta; sin existencia suficiente no se registra nada.
     */
    @Transactional
    public PosStockMovement transferOut(UUID pointOfSaleId, UUID articleId, int quantity, String transferFolio,
                                        String externalReference) {
        Instant now = clock.instant();
        if (stockRepository.decrease(pointOfSaleId, articleId, quantity, now) == 0) {
            throw new InsufficientPosStockException(pointOfSaleId, articleId, quantity);
        }
        PosStockMovement movement = movementRepository.saveAndFlush(
                PosStockMovement.transferOut(transferFolio, pointOfSaleId, articleId, quantity, externalReference, now));
        stockEvents.enqueue(pointOfSaleId, articleId, "TRASLADO", -quantity, onHand(pointOfSaleId, articleId),
                transferFolio, externalReference);
        return movement;
    }

    /** Recepciones (folios POS-ENT) de una tienda a partir de un instante, para que la tienda las baje. */
    @Transactional(readOnly = true)
    public List<PosStockMovement> listReceiptsSince(UUID pointOfSaleId, Instant since) {
        return movementRepository
                .findByPointOfSaleIdAndTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
                        pointOfSaleId, PosStockMovementType.ENTRADA, since);
    }

    /** Existencia del punto de venta después del movimiento (lo que el POS debe ver). */
    private int onHand(UUID pointOfSaleId, UUID articleId) {
        return stockRepository.findByPointOfSaleIdAndArticleId(pointOfSaleId, articleId)
                .map(PosStock::getQuantityOnHand).orElse(0);
    }

    private void ensureStockRow(UUID pointOfSaleId, UUID articleId, Instant now) {
        if (stockRepository.findByPointOfSaleIdAndArticleId(pointOfSaleId, articleId).isEmpty()) {
            stockRepository.saveAndFlush(PosStock.initialize(pointOfSaleId, articleId, now));
        }
    }

    @Transactional(readOnly = true)
    public List<PosStock> listStock(UUID pointOfSaleId) {
        return pointOfSaleId != null
                ? stockRepository.findByPointOfSaleIdOrderByArticleId(pointOfSaleId)
                : stockRepository.findAllByOrderByPointOfSaleId();
    }

    @Transactional(readOnly = true)
    public List<PosStockMovement> listMovements(UUID pointOfSaleId) {
        return pointOfSaleId != null
                ? movementRepository.findByPointOfSaleIdOrderByOccurredAtDesc(pointOfSaleId)
                : movementRepository.findAllByOrderByOccurredAtDesc();
    }
}
