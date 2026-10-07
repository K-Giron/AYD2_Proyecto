package com.megastore.sgologic.onlinestore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Traslado de mercadería de la tienda online por folio (TOL-NNNNNN), hacia el central u otra tienda. */
@Entity
@Table(name = "online_store_transfers")
public class OnlineTransfer {

    public enum Status { PENDIENTE, ENVIADA, RECHAZADO }

    public static final String CENTRAL = "CENTRAL";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 24)
    private String folio;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 32)
    private String destination;

    @Column(length = 255)
    private String reason;

    @Column(name = "requested_by", nullable = false, length = 160)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 16)
    private Status syncStatus;

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

    protected OnlineTransfer() {
    }

    static OnlineTransfer register(String folio, CatalogProduct product, int quantity, String destination,
                                   String reason, String requestedBy, Instant now) {
        OnlineTransfer transfer = new OnlineTransfer();
        transfer.id = UUID.randomUUID();
        transfer.folio = folio;
        transfer.productId = product.getProductId();
        transfer.sku = product.getSku();
        transfer.name = product.getName();
        transfer.quantity = quantity;
        transfer.destination = destination;
        transfer.reason = reason;
        transfer.requestedBy = requestedBy;
        transfer.syncStatus = Status.PENDIENTE;
        transfer.createdAt = now;
        return transfer;
    }

    void markSent(String centralFolio, String destinationFolio, Instant now) {
        this.syncStatus = Status.ENVIADA;
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
        this.syncStatus = Status.RECHAZADO;
        this.syncAttempts++;
        this.lastSyncError = truncate(error);
        this.syncedAt = now;
    }

    private static String truncate(String value) {
        return value == null ? null : value.substring(0, Math.min(value.length(), 500));
    }

    public UUID getId() { return id; }
    public String getFolio() { return folio; }
    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getDestination() { return destination; }
    public String getReason() { return reason; }
    public String getRequestedBy() { return requestedBy; }
    public Status getSyncStatus() { return syncStatus; }
    public String getCentralFolio() { return centralFolio; }
    public String getDestinationFolio() { return destinationFolio; }
    public int getSyncAttempts() { return syncAttempts; }
    public String getLastSyncError() { return lastSyncError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSyncedAt() { return syncedAt; }
}
