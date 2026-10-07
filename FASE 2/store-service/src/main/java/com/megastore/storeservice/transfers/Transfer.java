package com.megastore.storeservice.transfers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Salida de mercadería de esta tienda por folio, hacia el central o hacia otra tienda. */
@Entity
@Table(name = "transfers")
public class Transfer {

    public static final String CENTRAL = "CENTRAL";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 24)
    private String folio;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 32)
    private String destination;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false, length = 80)
    private String seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 16)
    private TransferSyncStatus syncStatus;

    @Column(name = "central_folio", length = 24)
    private String centralFolio;

    @Column(name = "destination_folio", length = 32)
    private String destinationFolio;

    @Column(name = "sync_attempts", nullable = false)
    private int syncAttempts;

    @Column(name = "last_sync_error", length = 500)
    private String lastSyncError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    protected Transfer() {
    }

    static Transfer register(String folio, UUID articleId, int quantity, String destination, String reason,
                             String seller, Instant now) {
        Transfer transfer = new Transfer();
        transfer.id = UUID.randomUUID();
        transfer.folio = folio;
        transfer.articleId = articleId;
        transfer.quantity = quantity;
        transfer.destination = destination;
        transfer.reason = reason;
        transfer.seller = seller;
        transfer.syncStatus = TransferSyncStatus.PENDIENTE;
        transfer.createdAt = now;
        return transfer;
    }

    void markSent(String centralFolio, String destinationFolio, Instant now) {
        this.syncStatus = TransferSyncStatus.ENVIADA;
        this.centralFolio = centralFolio;
        this.destinationFolio = destinationFolio;
        this.syncAttempts++;
        this.lastSyncError = null;
        this.syncedAt = now;
    }

    void markAttemptFailed(String error) {
        this.syncAttempts++;
        this.lastSyncError = truncate(error);
    }

    void markRejected(String error, Instant now) {
        this.syncStatus = TransferSyncStatus.RECHAZADO;
        this.syncAttempts++;
        this.lastSyncError = truncate(error);
        this.syncedAt = now;
    }

    private static String truncate(String error) {
        return error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }

    public UUID getId() { return id; }
    public String getFolio() { return folio; }
    public UUID getArticleId() { return articleId; }
    public int getQuantity() { return quantity; }
    public String getDestination() { return destination; }
    public String getReason() { return reason; }
    public String getSeller() { return seller; }
    public TransferSyncStatus getSyncStatus() { return syncStatus; }
    public String getCentralFolio() { return centralFolio; }
    public String getDestinationFolio() { return destinationFolio; }
    public int getSyncAttempts() { return syncAttempts; }
    public String getLastSyncError() { return lastSyncError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSyncedAt() { return syncedAt; }
}
