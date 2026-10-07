package com.megastore.inventoryservice.posevents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_stock_events")
public class PosStockEvent {

    public static final String PENDING = "PENDIENTE";
    public static final String RETRY = "REINTENTO";
    public static final String DELIVERED = "ENTREGADO";
    public static final String FAILED = "FALLIDO";
    public static final String NO_TERMINAL = "SIN_TERMINAL";

    @Id
    private UUID id;
    @Column(name = "point_of_sale_id", nullable = false)
    private UUID pointOfSaleId;
    @Column(name = "pos_code", nullable = false)
    private String posCode;
    @Column(name = "article_id", nullable = false)
    private UUID articleId;
    @Column(nullable = false)
    private String sku;
    @Column(name = "article_name", nullable = false)
    private String articleName;
    @Column(nullable = false)
    private String movement;
    @Column(nullable = false)
    private int delta;
    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;
    @Column(nullable = false)
    private String folio;
    private String reference;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "last_error")
    private String lastError;
    private String acknowledgement;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected PosStockEvent() {
    }

    static PosStockEvent of(UUID pointOfSaleId, String posCode, UUID articleId, String sku, String articleName,
                            String movement, int delta, int quantityAfter, String folio, String reference, Instant now) {
        PosStockEvent event = new PosStockEvent();
        event.id = UUID.randomUUID();
        event.pointOfSaleId = pointOfSaleId;
        event.posCode = posCode;
        event.articleId = articleId;
        event.sku = sku;
        event.articleName = articleName;
        event.movement = movement;
        event.delta = delta;
        event.quantityAfter = quantityAfter;
        event.folio = folio;
        event.reference = reference;
        event.status = PENDING;
        event.nextAttemptAt = now;
        event.createdAt = now;
        return event;
    }

    void delivered(String acknowledgement, Instant now) {
        this.attempts += 1;
        this.status = DELIVERED;
        this.acknowledgement = acknowledgement;
        this.lastError = null;
        this.deliveredAt = now;
    }

    /** Falla de red o del POS: reintento con espera creciente; al agotarse, FALLIDO. */
    void failed(String error, long[] retryDelaysSeconds, Instant now) {
        this.attempts += 1;
        this.lastError = error.length() > 400 ? error.substring(0, 400) : error;
        if (attempts <= retryDelaysSeconds.length) {
            this.status = RETRY;
            this.nextAttemptAt = now.plusSeconds(retryDelaysSeconds[attempts - 1]);
        } else {
            this.status = FAILED;
        }
    }

    void noTerminal(Instant now) {
        this.status = NO_TERMINAL;
        this.lastError = "El punto de venta " + posCode + " no tiene sistema conectado todavía.";
        this.nextAttemptAt = now.plusSeconds(60);
    }

    /** Reenvío manual de un evento fallido (el POS volvió): reintentos desde cero. */
    void reopen(Instant now) {
        this.status = PENDING;
        this.attempts = 0;
        this.nextAttemptAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPointOfSaleId() { return pointOfSaleId; }
    public String getPosCode() { return posCode; }
    public UUID getArticleId() { return articleId; }
    public String getSku() { return sku; }
    public String getArticleName() { return articleName; }
    public String getMovement() { return movement; }
    public int getDelta() { return delta; }
    public int getQuantityAfter() { return quantityAfter; }
    public String getFolio() { return folio; }
    public String getReference() { return reference; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public String getAcknowledgement() { return acknowledgement; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
