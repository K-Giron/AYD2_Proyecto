package com.megastore.storeservice.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mercadería que el central envió a la tienda (su folio POS-ENT). Llega
 * EN_TRANSITO y solo suma a la existencia cuando la tienda confirma el
 * folio de envío ({@code originFolio}: SAL-… del central o TRA-… de otra tienda).
 */
@Entity
@Table(name = "store_receipts")
public class StoreReceipt {

    public enum Status { EN_TRANSITO, RECIBIDO }

    @Id
    private String folio;

    @Column(name = "origin_folio", length = 32)
    private String originFolio;

    /** "CENTRAL" o el código de la tienda que trasladó. */
    @Column(length = 32)
    private String origin;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

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

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "received_by", length = 80)
    private String receivedBy;

    protected StoreReceipt() {
    }

    static StoreReceipt inTransit(IncomingReceipt incoming) {
        StoreReceipt receipt = new StoreReceipt();
        receipt.folio = incoming.folio();
        receipt.originFolio = incoming.originFolio();
        receipt.origin = incoming.origin();
        receipt.articleId = incoming.articleId();
        receipt.sku = incoming.sku();
        receipt.name = incoming.name();
        receipt.description = incoming.description();
        receipt.price = incoming.price();
        receipt.quantity = incoming.quantity();
        receipt.status = Status.EN_TRANSITO;
        receipt.occurredAt = incoming.occurredAt();
        return receipt;
    }

    void markReceived(String seller, Instant now) {
        this.status = Status.RECIBIDO;
        this.receivedBy = seller;
        this.appliedAt = now;
    }

    /** El folio que la tienda escribe al recibir: el de envío si existe, si no el propio. */
    public String getShipmentFolio() { return originFolio == null ? folio : originFolio; }

    public String getFolio() { return folio; }
    public String getOriginFolio() { return originFolio; }
    public String getOrigin() { return origin; }
    public UUID getArticleId() { return articleId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getAppliedAt() { return appliedAt; }
    public String getReceivedBy() { return receivedBy; }
}
