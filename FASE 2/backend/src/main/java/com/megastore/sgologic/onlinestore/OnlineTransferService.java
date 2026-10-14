package com.megastore.sgologic.onlinestore;

import com.megastore.sgologic.inventory.InsufficientOnHandStockException;
import com.megastore.sgologic.inventory.InventoryItemNotFoundException;
import com.megastore.sgologic.inventory.InventoryMovementService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Traslados de la tienda online: la mercadería sale de CD-WEB al emitir el
 * folio (solo unidades disponibles, nunca las reservadas por una compra en
 * curso) y se reporta al central, que la ingresa a su almacén o la manda a
 * otra tienda. Si el central lo rechaza, vuelve a CD-WEB; si no hay
 * conexión, queda pendiente y se reintenta.
 */
@Service
public class OnlineTransferService {

    private final OnlineTransferRepository repository;
    private final CatalogProductRepository productRepository;
    private final InventoryMovementService inventoryMovementService;
    private final CentralReceiptsClient central;
    private final OnlineSyncLog syncLog;
    private final OnlineStoreProperties properties;
    private final Clock clock;
    // push() habla con el central fuera de transacción; cada cambio de estado va en la suya.
    private final TransactionTemplate tx;

    OnlineTransferService(OnlineTransferRepository repository, CatalogProductRepository productRepository,
                          InventoryMovementService inventoryMovementService, CentralReceiptsClient central,
                          OnlineSyncLog syncLog, OnlineStoreProperties properties, Clock clock,
                          PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
        this.repository = repository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.central = central;
        this.syncLog = syncLog;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public OnlineTransfer create(CreateOnlineTransferRequest request, String requestedBy) {
        String destination = request.destination().trim().toUpperCase(Locale.ROOT);
        if (destination.equals(properties.getPointOfSaleCode())) {
            throw new OnlineStoreException(HttpStatus.BAD_REQUEST, "INVALID_DESTINATION",
                    "La tienda online no puede trasladarse mercadería a sí misma.");
        }
        CatalogProduct product = productRepository.findById(request.productId()).orElseThrow(() ->
                new OnlineStoreException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "La tienda online no tiene ese producto."));
        String folio = "TOL-" + String.format("%06d", repository.nextFolioSequence());
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        try {
            inventoryMovementService.issueFromLocation(product.getProductId(), properties.getLocationCode(),
                    request.quantity(), "Traslado " + folio + " hacia " + destination
                            + (reason == null ? "" : ": " + reason), UUID.randomUUID());
        } catch (InsufficientOnHandStockException | InventoryItemNotFoundException notEnough) {
            throw new OnlineStoreException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "No hay " + request.quantity() + " unidad(es) disponibles de " + product.getName()
                            + " en la tienda online (lo reservado por compras en curso no se puede trasladar).");
        }
        return repository.saveAndFlush(OnlineTransfer.register(folio, product, request.quantity(), destination,
                reason, requestedBy, clock.instant()));
    }

    /** Reporta un traslado al central. Devuelve false si no hubo conexión (queda pendiente). */
    public boolean push(UUID transferId) {
        OnlineTransfer transfer = get(transferId);
        if (transfer.getSyncStatus() != OnlineTransfer.Status.PENDIENTE) {
            return true;
        }
        try {
            CentralReceiptsClient.TransferReceipt receipt = central.registerTransfer(new CentralReceiptsClient.TransferBody(
                    transfer.getFolio(), transfer.getProductId(), transfer.getQuantity(), transfer.getDestination(),
                    transfer.getReason(), transfer.getRequestedBy()));
            markSent(transferId, receipt.centralFolio(), receipt.destinationFolio());
            syncLog.record(OnlineSyncLog.SUBIDA, OnlineSyncLog.TRASLADO, transfer.getFolio(), true,
                    "Traslado " + transfer.getFolio() + ": " + transfer.getQuantity() + " × " + transfer.getSku()
                            + " hacia " + label(transfer.getDestination()) + ", registrado como " + receipt.centralFolio()
                            + " (entrada " + receipt.destinationFolio() + ").");
            return true;
        } catch (OnlineStoreException error) {
            if ("TRANSFER_REJECTED".equals(error.getCode())) {
                reject(transferId, error.getMessage());
                syncLog.record(OnlineSyncLog.SUBIDA, OnlineSyncLog.TRASLADO, transfer.getFolio(), false,
                        "El central rechazó el traslado; la mercadería volvió a la tienda online. " + error.getMessage());
                return true;
            }
            markAttemptFailed(transferId, error.getMessage());
            if (transfer.getSyncAttempts() == 0) {
                syncLog.record(OnlineSyncLog.SUBIDA, OnlineSyncLog.TRASLADO, transfer.getFolio(), false,
                        error.getMessage() + " El traslado queda pendiente y se reintenta solo.");
            }
            return false;
        }
    }

    /** Reintenta los pendientes en orden; se detiene en la primera falta de conexión. */
    public void pushPending() {
        for (OnlineTransfer transfer : repository.findBySyncStatusOrderByCreatedAtAsc(OnlineTransfer.Status.PENDIENTE)) {
            if (!push(transfer.getId())) {
                return;
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OnlineTransfer> list() {
        return repository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<CentralReceiptsClient.PointOfSale> destinations() {
        return central.pointsOfSale().stream()
                .filter(pos -> !properties.getPointOfSaleCode().equals(pos.code()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OnlineTransfer get(UUID transferId) {
        return repository.findById(transferId).orElseThrow(() -> new OnlineStoreException(HttpStatus.NOT_FOUND,
                "TRANSFER_NOT_FOUND", "No existe el traslado " + transferId + "."));
    }

    private void markSent(UUID transferId, String centralFolio, String destinationFolio) {
        tx.executeWithoutResult(status -> get(transferId).markSent(centralFolio, destinationFolio, clock.instant()));
    }

    private void markAttemptFailed(UUID transferId, String error) {
        tx.executeWithoutResult(status -> get(transferId).markAttemptFailed(error));
    }

    /** El central no lo aceptó: la mercadería nunca salió de la tienda online, vuelve a CD-WEB (todo o nada). */
    private void reject(UUID transferId, String error) {
        tx.executeWithoutResult(status -> {
            OnlineTransfer transfer = get(transferId);
            if (transfer.getSyncStatus() != OnlineTransfer.Status.PENDIENTE) {
                return;
            }
            transfer.markRejected(error, clock.instant());
            repository.saveAndFlush(transfer);
            inventoryMovementService.receiveAtLocation(transfer.getProductId(), properties.getLocationCode(),
                    transfer.getQuantity(), "Devolución por traslado rechazado " + transfer.getFolio(), UUID.randomUUID());
        });
    }

    private static String label(String destination) {
        return OnlineTransfer.CENTRAL.equals(destination) ? "el almacén central" : destination;
    }
}
