package com.megastore.inventoryservice.storesync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Traslado por folio desde una tienda hacia el almacén central ("CENTRAL") o hacia otra tienda. */
@Entity
@Table(name = "store_transfers")
public class StoreTransfer {

    public static final String CENTRAL = "CENTRAL";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "central_folio", nullable = false, updatable = false, length = 16)
    private String centralFolio;

    @Column(name = "origin_pos_id", nullable = false, updatable = false)
    private UUID originPosId;

    @Column(name = "origin_pos_code", nullable = false, updatable = false, length = 32)
    private String originPosCode;

    @Column(name = "local_folio", nullable = false, updatable = false, length = 24)
    private String localFolio;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "destination", nullable = false, updatable = false, length = 32)
    private String destination;

    @Column(name = "destination_folio", length = 24)
    private String destinationFolio;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "seller", nullable = false, updatable = false, length = 80)
    private String seller;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected StoreTransfer() {
        // Requerido por JPA.
    }

    static StoreTransfer register(String centralFolio, UUID originPosId, String originPosCode,
                                  RegisterStoreTransferRequest request, String destinationFolio, Instant now) {
        StoreTransfer transfer = new StoreTransfer();
        transfer.id = UUID.randomUUID();
        transfer.centralFolio = centralFolio;
        transfer.originPosId = originPosId;
        transfer.originPosCode = originPosCode;
        transfer.localFolio = request.localFolio();
        transfer.articleId = request.articleId();
        transfer.quantity = request.quantity();
        transfer.destination = request.destination();
        transfer.destinationFolio = destinationFolio;
        transfer.reason = request.reason();
        transfer.seller = request.seller();
        transfer.occurredAt = now;
        return transfer;
    }

    public UUID getId() { return id; }
    public String getCentralFolio() { return centralFolio; }
    public UUID getOriginPosId() { return originPosId; }
    public String getOriginPosCode() { return originPosCode; }
    public String getLocalFolio() { return localFolio; }
    public UUID getArticleId() { return articleId; }
    public int getQuantity() { return quantity; }
    public String getDestination() { return destination; }
    public String getDestinationFolio() { return destinationFolio; }
    public String getReason() { return reason; }
    public String getSeller() { return seller; }
    public Instant getOccurredAt() { return occurredAt; }
}
