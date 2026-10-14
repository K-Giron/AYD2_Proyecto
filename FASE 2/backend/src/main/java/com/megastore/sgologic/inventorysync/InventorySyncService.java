package com.megastore.sgologic.inventorysync;

import com.megastore.sgologic.onlinestore.OnlineSyncLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orquesta el reintento 2/8/32 s hacia inventory-service (mismo patrón
 * que LOG-03 en {@code logistics.ShipmentService}) y la bandeja de
 * fallidos cuando se agotan los reintentos o el nombre del producto no
 * resuelve a ningún artículo. Una falla aquí nunca bloquea la venta: la
 * orden ya quedó confirmada en el monolito antes de que este servicio
 * actúe.
 */
@Service
class InventorySyncService {

    private static final Logger log = LoggerFactory.getLogger(InventorySyncService.class);

    /** Reintentos hacia inventory-service en 2, 8 y 32 segundos. */
    static final long[] RETRY_DELAYS_SECONDS = {2L, 8L, 32L};

    private final InventoryStockPort port;
    private final InventoryBridgeDeadLetterRepository deadLetterRepository;
    private final TaskScheduler retryScheduler;
    private final OnlineSyncLog onlineSyncLog;
    private final Clock clock;

    InventorySyncService(InventoryStockPort port, InventoryBridgeDeadLetterRepository deadLetterRepository,
                          @Qualifier("inventorySyncRetryScheduler") TaskScheduler retryScheduler,
                          OnlineSyncLog onlineSyncLog, Clock clock) {
        this.onlineSyncLog = onlineSyncLog;
        this.port = port;
        this.deadLetterRepository = deadLetterRepository;
        this.retryScheduler = retryScheduler;
        this.clock = clock;
    }

    /**
     * Programa el primer intento solo después de que la transacción que
     * confirmó la orden (y leyó sus líneas) haga commit — si se programara
     * dentro de la transacción, el hilo del planificador podría ejecutarse
     * antes del commit. Mismo patrón que
     * {@code ShipmentService.scheduleLabelRequestAfterCommit}.
     */
    void triggerSync(UUID orderId, List<SoldLine> lines, String storePointOfSaleCode) {
        if (lines.isEmpty()) {
            return;
        }
        Runnable scheduleFirstAttempt = () -> retryScheduler.schedule(
                () -> syncOrder(orderId, lines, storePointOfSaleCode, 1), Instant.now());
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scheduleFirstAttempt.run();
                }
            });
        } else {
            scheduleFirstAttempt.run();
        }
    }

    /**
     * {@code storePointOfSaleCode} nulo = venta web (descuenta la
     * existencia propia de "Tienda Online", POS-ONLINE); con código = venta
     * en esa tienda física (descuenta la existencia propia de la tienda).
     */
    void syncOrder(UUID orderId, List<SoldLine> lines, String storePointOfSaleCode, int attemptNumber) {
        List<SoldLine> retryable = new ArrayList<>();
        for (SoldLine line : lines) {
            try {
                String folio = storePointOfSaleCode == null
                        ? port.registerSale(line, orderId)
                        : port.registerStoreSale(line, orderId, storePointOfSaleCode);
                log.info("Venta sincronizada con inventory-service: orden={}, canal={}, producto={}, folio={}.",
                        orderId, storePointOfSaleCode == null ? "web" : storePointOfSaleCode,
                        line.productName(), folio);
                logWebSale(storePointOfSaleCode, orderId, true, line.quantity() + " × " + line.productName()
                        + " descontado de POS-ONLINE en el almacén central (" + folio + ").");
            } catch (InventoryArticleNotFoundException | InventoryStoreSaleRejectedException dataProblem) {
                log.error("Venta sin poder reflejarse en inventory-service (dato, no red): orden={}, producto={}: {}",
                        orderId, line.productName(), dataProblem.getMessage());
                deadLetterRepository.save(InventoryBridgeDeadLetter.of(orderId, line.productId(),
                        truncate(dataProblem.getMessage()), attemptNumber, clock.instant()));
                logWebSale(storePointOfSaleCode, orderId, false, line.quantity() + " × " + line.productName()
                        + " no se pudo reportar al central (queda en la bandeja de fallidos): " + dataProblem.getMessage());
            } catch (InventoryServiceUnavailableException unavailable) {
                log.warn("Falla hablando con inventory-service (intento {}) para orden={}, producto={}: {}.",
                        attemptNumber, orderId, line.productName(), unavailable.getMessage());
                retryable.add(line);
            }
        }
        if (!retryable.isEmpty()) {
            handleRetryableFailure(orderId, retryable, storePointOfSaleCode, attemptNumber);
        }
    }

    /** Solo las ventas web (POS-ONLINE) van a la bitácora de la tienda online. */
    private void logWebSale(String storePointOfSaleCode, UUID orderId, boolean ok, String detail) {
        if (storePointOfSaleCode != null) {
            return;
        }
        try {
            onlineSyncLog.record(OnlineSyncLog.SUBIDA, OnlineSyncLog.VENTA, orderId.toString(), ok,
                    "Orden " + orderId.toString().substring(0, 8) + ": " + detail);
        } catch (RuntimeException ignored) {
            // La bitácora nunca debe afectar la sincronización de la venta.
        }
    }

    private static String truncate(String message) {
        return message == null || message.length() <= 255 ? message : message.substring(0, 255);
    }

    private void handleRetryableFailure(UUID orderId, List<SoldLine> lines, String storePointOfSaleCode,
                                        int attemptsSoFar) {
        int nextRetryIndex = attemptsSoFar - 1;
        if (nextRetryIndex < RETRY_DELAYS_SECONDS.length) {
            long delaySeconds = RETRY_DELAYS_SECONDS[nextRetryIndex];
            retryScheduler.schedule(() -> syncOrder(orderId, lines, storePointOfSaleCode, attemptsSoFar + 1),
                    Instant.now().plusSeconds(delaySeconds));
        } else {
            log.error("Se agotaron los reintentos hacia inventory-service para la orden {}.", orderId);
            logWebSale(storePointOfSaleCode, orderId, false,
                    "Sin conexión con el almacén central tras " + attemptsSoFar + " intentos; la venta queda en la bandeja de fallidos.");
            for (SoldLine line : lines) {
                deadLetterRepository.save(InventoryBridgeDeadLetter.of(orderId, line.productId(),
                        "Agotados los reintentos contactando inventory-service", attemptsSoFar, clock.instant()));
            }
        }
    }
}
