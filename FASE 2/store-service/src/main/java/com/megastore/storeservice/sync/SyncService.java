package com.megastore.storeservice.sync;

import com.megastore.storeservice.inventory.IncomingReceipt;
import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.sales.Sale;
import com.megastore.storeservice.sales.SaleService;
import com.megastore.storeservice.sales.SaleSyncStatus;
import com.megastore.storeservice.transfers.Transfer;
import com.megastore.storeservice.transfers.TransferService;
import com.megastore.storeservice.transfers.TransferSyncStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sincronización tienda ⇄ central. Nunca corre dentro de la transacción
 * de una venta: la venta ya quedó guardada localmente y esto solo intenta
 * avisarle al central. Todo intento queda en la bitácora de sincronización.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final CentralClient central;
    private final SaleService saleService;
    private final TransferService transferService;
    private final InventoryService inventoryService;
    private final SyncLog syncLog;
    // El programador y el botón "Sincronizar" pueden coincidir: una sola subida/bajada a la vez.
    private final ReentrantLock pushLock = new ReentrantLock();
    private final ReentrantLock pullLock = new ReentrantLock();
    private volatile boolean pullFailing;
    private volatile Instant lastContactAt;

    public SyncService(CentralClient central, SaleService saleService, TransferService transferService,
                       InventoryService inventoryService, SyncLog syncLog) {
        this.central = central;
        this.saleService = saleService;
        this.transferService = transferService;
        this.inventoryService = inventoryService;
        this.syncLog = syncLog;
    }

    /** Sube la factura de una venta. Devuelve false si el central no estaba disponible. */
    public boolean pushInvoice(UUID saleId) {
        pushLock.lock();
        try {
            return doPushInvoice(saleService.get(saleId));
        } finally {
            pushLock.unlock();
        }
    }

    public boolean pushTransfer(UUID transferId) {
        pushLock.lock();
        try {
            return doPushTransfer(transferService.get(transferId));
        } finally {
            pushLock.unlock();
        }
    }

    /** Reintenta todo lo pendiente, en orden; se detiene en la primera falta de conexión. */
    public void pushPending() {
        if (!pushLock.tryLock()) {
            return;
        }
        try {
            for (Sale sale : saleService.pending()) {
                if (!doPushInvoice(sale)) {
                    return;
                }
            }
            for (Transfer transfer : transferService.pending()) {
                if (!doPushTransfer(transfer)) {
                    return;
                }
            }
        } finally {
            pushLock.unlock();
        }
    }

    /**
     * Baja del central lo que le enviaron a esta tienda y lo deja EN_TRANSITO
     * (una vez por folio); suma a la existencia solo cuando la tienda confirma
     * el folio. {@code manual}: lo pidió alguien con el botón, así que también
     * se registra cuando no hay nada nuevo.
     */
    public int pullReceipts(boolean manual) {
        pullLock.lock();
        try {
            List<IncomingReceipt> receipts = central.receipts(inventoryService.latestReceiptAt().orElse(null));
            lastContactAt = Instant.now();
            int applied = 0;
            for (IncomingReceipt receipt : receipts) {
                if (inventoryService.registerIncoming(receipt)) {
                    applied++;
                    syncLog.record(SyncLog.BAJADA, "RECEPCION", receipt.originFolio(), true,
                            "En tránsito: " + receipt.quantity() + " de " + receipt.sku() + " (" + receipt.name()
                                    + ") enviado por " + ("CENTRAL".equals(receipt.origin()) ? "el almacén central" : receipt.origin())
                                    + " con folio " + receipt.originFolio() + ". Suma a la existencia al confirmar la recepción.");
                }
            }
            if (pullFailing || (manual && applied == 0)) {
                syncLog.record(SyncLog.BAJADA, "RECEPCION", null, true, pullFailing
                        ? "Conexión con el central restablecida; " + applied + " envío(s) nuevo(s) en tránsito."
                        : "Sin envíos nuevos del central.");
            }
            pullFailing = false;
            return applied;
        } catch (CentralUnavailableException | CentralRejectedException error) {
            // Mientras el central siga caído no se repite el mismo error cada 15 s.
            if (!pullFailing || manual) {
                syncLog.record(SyncLog.BAJADA, "RECEPCION", null, false, error.getMessage());
            }
            pullFailing = true;
            if (manual) {
                throw error;
            }
            return 0;
        } finally {
            pullLock.unlock();
        }
    }

    public boolean isCentralReachable() {
        return !pullFailing && lastContactAt != null;
    }

    public Instant getLastContactAt() {
        return lastContactAt;
    }

    private boolean doPushInvoice(Sale sale) {
        if (sale.getSyncStatus() == SaleSyncStatus.ENVIADA) {
            return true;
        }
        try {
            CentralClient.InvoiceReceipt receipt = central.uploadInvoice(invoiceBody(sale));
            lastContactAt = Instant.now();
            saleService.markSent(sale.getId(), receipt.centralFolio());
            syncLog.record(SyncLog.SUBIDA, "FACTURA", sale.getFolio(), true,
                    "Factura " + sale.getFolio() + " (" + sale.getTaxId() + ", Q" + sale.getTotal()
                            + ") recibida por el central como " + receipt.centralFolio() + ".");
            return true;
        } catch (CentralUnavailableException offline) {
            saleService.markAttemptFailed(sale.getId(), offline.getMessage(), false);
            syncLog.record(SyncLog.SUBIDA, "FACTURA", sale.getFolio(), false,
                    offline.getMessage() + " La factura queda pendiente y se reintenta sola.");
            return false;
        } catch (CentralRejectedException rejected) {
            saleService.markAttemptFailed(sale.getId(), rejected.getMessage(), true);
            syncLog.record(SyncLog.SUBIDA, "FACTURA", sale.getFolio(), false,
                    "El central rechazó la factura: " + rejected.getMessage());
            return true;
        } catch (RuntimeException unexpected) {
            log.warn("Error inesperado subiendo la factura {}", sale.getFolio(), unexpected);
            saleService.markAttemptFailed(sale.getId(), unexpected.getMessage(), false);
            syncLog.record(SyncLog.SUBIDA, "FACTURA", sale.getFolio(), false,
                    "Error inesperado: " + unexpected.getMessage());
            return false;
        }
    }

    private boolean doPushTransfer(Transfer transfer) {
        if (transfer.getSyncStatus() != TransferSyncStatus.PENDIENTE) {
            return true;
        }
        try {
            CentralClient.TransferReceipt receipt = central.registerTransfer(transferBody(transfer));
            lastContactAt = Instant.now();
            transferService.markSent(transfer.getId(), receipt.centralFolio(), receipt.destinationFolio());
            syncLog.record(SyncLog.SUBIDA, "TRASLADO", transfer.getFolio(), true,
                    "Traslado " + transfer.getFolio() + " hacia " + transfer.getDestination()
                            + " registrado como " + receipt.centralFolio() + " (entrada " + receipt.destinationFolio() + ").");
            return true;
        } catch (CentralUnavailableException offline) {
            transferService.markAttemptFailed(transfer.getId(), offline.getMessage());
            syncLog.record(SyncLog.SUBIDA, "TRASLADO", transfer.getFolio(), false,
                    offline.getMessage() + " El traslado queda pendiente y se reintenta solo.");
            return false;
        } catch (CentralRejectedException rejected) {
            transferService.reject(transfer.getId(), rejected.getMessage());
            syncLog.record(SyncLog.SUBIDA, "TRASLADO", transfer.getFolio(), false,
                    "El central rechazó el traslado; la mercadería vuelve a la existencia de la tienda. "
                            + rejected.getMessage());
            return true;
        } catch (RuntimeException unexpected) {
            log.warn("Error inesperado subiendo el traslado {}", transfer.getFolio(), unexpected);
            transferService.markAttemptFailed(transfer.getId(), unexpected.getMessage());
            syncLog.record(SyncLog.SUBIDA, "TRASLADO", transfer.getFolio(), false,
                    "Error inesperado: " + unexpected.getMessage());
            return false;
        }
    }

    private static Map<String, Object> invoiceBody(Sale sale) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ticketFolio", sale.getFolio());
        body.put("issuedAt", sale.getCreatedAt().toString());
        body.put("seller", sale.getSeller());
        body.put("taxId", sale.getTaxId());
        body.put("customerName", sale.getCustomerName());
        body.put("paymentMethod", sale.getPaymentMethod().name());
        body.put("total", sale.getTotal());
        body.put("lines", sale.getLines().stream()
                .map(line -> new CentralClient.InvoiceLine(line.getArticleId(), line.getSku(), line.getName(),
                        line.getUnitPrice(), line.getQuantity(), line.getLineTotal()))
                .toList());
        return body;
    }

    private static Map<String, Object> transferBody(Transfer transfer) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("localFolio", transfer.getFolio());
        body.put("articleId", transfer.getArticleId());
        body.put("quantity", transfer.getQuantity());
        body.put("destination", transfer.getDestination());
        if (transfer.getReason() != null) {
            body.put("reason", transfer.getReason());
        }
        body.put("seller", transfer.getSeller());
        return body;
    }
}
