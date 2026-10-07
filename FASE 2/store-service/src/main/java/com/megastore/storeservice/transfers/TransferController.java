package com.megastore.storeservice.transfers;

import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.inventory.StoreArticle;
import com.megastore.storeservice.sync.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/store/transfers")
public class TransferController {

    private final TransferService transferService;
    private final SyncService syncService;
    private final InventoryService inventoryService;

    public TransferController(TransferService transferService, SyncService syncService,
                              InventoryService inventoryService) {
        this.transferService = transferService;
        this.syncService = syncService;
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request) {
        Transfer transfer = transferService.create(request);
        syncService.pushTransfer(transfer.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(transferService.get(transfer.getId())));
    }

    @GetMapping
    public List<TransferResponse> list() {
        return transferService.list().stream().map(this::toResponse).toList();
    }

    private TransferResponse toResponse(Transfer transfer) {
        StoreArticle article = inventoryService.getArticle(transfer.getArticleId());
        return new TransferResponse(transfer.getId(), transfer.getFolio(), transfer.getArticleId(), article.getSku(),
                article.getName(), transfer.getQuantity(), transfer.getDestination(), transfer.getReason(),
                transfer.getSeller(), transfer.getSyncStatus(), transfer.getCentralFolio(),
                transfer.getDestinationFolio(), transfer.getSyncAttempts(), transfer.getLastSyncError(),
                transfer.getCreatedAt(), transfer.getSyncedAt());
    }

    public record TransferResponse(UUID id, String folio, UUID articleId, String sku, String articleName,
                                   int quantity, String destination, String reason, String seller,
                                   TransferSyncStatus syncStatus, String centralFolio, String destinationFolio,
                                   int syncAttempts, String lastSyncError, Instant createdAt, Instant syncedAt) {
    }
}
