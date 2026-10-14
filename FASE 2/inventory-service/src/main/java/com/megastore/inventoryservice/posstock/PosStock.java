package com.megastore.inventoryservice.posstock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Existencia propia de un punto de venta (distinta de la del almacén
 * central): sube cuando llega un folio de salida del almacén dirigido a
 * ese punto de venta. Nunca se edita directo.
 */
@Entity
@Table(name = "pos_stock")
public class PosStock {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "point_of_sale_id", nullable = false, updatable = false)
    private UUID pointOfSaleId;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PosStock() {
        // Requerido por JPA.
    }

    static PosStock initialize(UUID pointOfSaleId, UUID articleId, Instant now) {
        PosStock stock = new PosStock();
        stock.id = UUID.randomUUID();
        stock.pointOfSaleId = pointOfSaleId;
        stock.articleId = articleId;
        stock.quantityOnHand = 0;
        stock.updatedAt = now;
        return stock;
    }

    public UUID getId() { return id; }
    public UUID getPointOfSaleId() { return pointOfSaleId; }
    public UUID getArticleId() { return articleId; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public Instant getUpdatedAt() { return updatedAt; }
}
