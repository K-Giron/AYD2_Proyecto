package com.megastore.inventoryservice.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Existencia actual de un artículo en el almacén central. Nunca se edita
 * directo: solo cambia a través de un {@link StockMovement} (entrada o
 * salida), que es quien deja el folio como evidencia del movimiento.
 */
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Stock() {
        // Requerido por JPA.
    }

    static Stock initialize(UUID articleId, UUID warehouseId, Instant now) {
        Stock stock = new Stock();
        stock.articleId = articleId;
        stock.warehouseId = warehouseId;
        stock.quantityOnHand = 0;
        stock.updatedAt = now;
        return stock;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
