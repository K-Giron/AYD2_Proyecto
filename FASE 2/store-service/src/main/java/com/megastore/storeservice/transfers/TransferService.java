package com.megastore.storeservice.transfers;

import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.inventory.StoreArticle;
import com.megastore.storeservice.shared.StoreBusinessException;
import com.megastore.storeservice.shared.StoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransferService {

    private final TransferRepository repository;
    private final InventoryService inventoryService;
    private final StoreProperties properties;
    private final Clock clock;

    public TransferService(TransferRepository repository, InventoryService inventoryService,
                           StoreProperties properties, Clock clock) {
        this.repository = repository;
        this.inventoryService = inventoryService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * La mercadería sale del almacén de la tienda en el momento del folio
     * (ya no se puede vender); el central la recibe cuando el folio sube.
     */
    @Transactional
    public Transfer create(CreateTransferRequest request) {
        String destination = request.destination().trim().toUpperCase(Locale.ROOT);
        if (destination.equals(properties.code())) {
            throw new StoreBusinessException(HttpStatus.BAD_REQUEST, "INVALID_DESTINATION",
                    "Una tienda no puede trasladarse mercadería a sí misma.");
        }
        StoreArticle article = inventoryService.getArticle(request.articleId());
        inventoryService.take(article, request.quantity());
        String folio = "TRS-" + String.format("%06d", repository.nextTransferSequence());
        return repository.saveAndFlush(Transfer.register(folio, article.getId(), request.quantity(), destination,
                request.reason() == null || request.reason().isBlank() ? null : request.reason().trim(),
                request.seller().trim(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public Transfer get(UUID transferId) {
        return repository.findById(transferId).orElseThrow(() -> new StoreBusinessException(
                HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND", "No existe el traslado " + transferId + "."));
    }

    @Transactional(readOnly = true)
    public List<Transfer> list() {
        return repository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Transfer> pending() {
        return repository.findBySyncStatusOrderByCreatedAtAsc(TransferSyncStatus.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countBySyncStatus(TransferSyncStatus.PENDIENTE);
    }

    @Transactional
    public void markSent(UUID transferId, String centralFolio, String destinationFolio) {
        get(transferId).markSent(centralFolio, destinationFolio, clock.instant());
    }

    @Transactional
    public void markAttemptFailed(UUID transferId, String error) {
        get(transferId).markAttemptFailed(error);
    }

    /** El central no lo aceptó: la mercadería nunca salió, vuelve a la existencia de la tienda. */
    @Transactional
    public void reject(UUID transferId, String error) {
        Transfer transfer = get(transferId);
        if (transfer.getSyncStatus() != TransferSyncStatus.PENDIENTE) {
            return;
        }
        transfer.markRejected(error, clock.instant());
        inventoryService.giveBack(transfer.getArticleId(), transfer.getQuantity());
    }
}
