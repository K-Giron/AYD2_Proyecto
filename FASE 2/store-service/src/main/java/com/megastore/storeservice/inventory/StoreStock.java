package com.megastore.storeservice.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Existencia local. Solo cambia por recepción, venta o traslado; nunca se edita a mano. */
@Entity
@Table(name = "store_stock")
public class StoreStock {

    @Id
    @Column(name = "article_id")
    private UUID articleId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoreStock() {
    }

    static StoreStock empty(UUID articleId, Instant now) {
        StoreStock stock = new StoreStock();
        stock.articleId = articleId;
        stock.quantity = 0;
        stock.updatedAt = now;
        return stock;
    }

    public UUID getArticleId() { return articleId; }
    public int getQuantity() { return quantity; }
    public Instant getUpdatedAt() { return updatedAt; }
}
