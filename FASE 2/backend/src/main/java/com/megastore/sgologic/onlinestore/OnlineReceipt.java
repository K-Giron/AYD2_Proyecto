package com.megastore.sgologic.onlinestore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mercadería enviada a la tienda online; suma al inventario solo al confirmar su folio. */
@Entity
@Table(name = "online_store_receipts")
public class OnlineReceipt {

    public enum Status { EN_TRANSITO, RECIBIDO }

    @Id
    private String folio;

    @Column(name = "shipment_folio", nullable = false, length = 32)
    private String shipmentFolio;

    @Column(nullable = false, length = 32)
    private String origin;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_by", length = 160)
    private String receivedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    protected OnlineReceipt() {
    }

    static OnlineReceipt inTransit(IncomingReceipt incoming) {
        OnlineReceipt receipt = new OnlineReceipt();
        receipt.folio = incoming.folio();
        receipt.shipmentFolio = incoming.originFolio() == null ? incoming.folio() : incoming.originFolio();
        receipt.origin = incoming.origin() == null ? "CENTRAL" : incoming.origin();
        receipt.productId = incoming.articleId();
        receipt.sku = incoming.sku();
        receipt.name = incoming.name();
        receipt.description = incoming.description();
        receipt.price = incoming.price();
        receipt.quantity = incoming.quantity();
        receipt.status = Status.EN_TRANSITO;
        receipt.occurredAt = incoming.occurredAt();
        return receipt;
    }

    void markReceived(String actor, Instant now) {
        this.status = Status.RECIBIDO;
        this.receivedBy = actor;
        this.receivedAt = now;
    }

    public String getFolio() { return folio; }
    public String getShipmentFolio() { return shipmentFolio; }
    public String getOrigin() { return origin; }
    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getReceivedBy() { return receivedBy; }
    public Instant getReceivedAt() { return receivedAt; }
}
