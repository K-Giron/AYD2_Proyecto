package com.megastore.sgologic.inventorysync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Cola de ventas web que no se pudieron reflejar en inventory-service:
 * agotados los reintentos de red, o el nombre del producto no tiene
 * artículo correspondiente en ese catálogo. {@code orderId} se guarda
 * solo por valor (no FK): este módulo no es dueño de la tabla de
 * órdenes.
 */
@Entity
@Table(name = "inventory_bridge_dead_letters")
public class InventoryBridgeDeadLetter {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", updatable = false)
    private UUID productId;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected InventoryBridgeDeadLetter() {
        // Requerido por JPA.
    }

    private InventoryBridgeDeadLetter(UUID id, UUID orderId, UUID productId, String reason, int attempts,
                                       Instant registeredAt) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.reason = reason;
        this.attempts = attempts;
        this.registeredAt = registeredAt;
    }

    public static InventoryBridgeDeadLetter of(UUID orderId, UUID productId, String reason, int attempts,
                                                Instant registeredAt) {
        return new InventoryBridgeDeadLetter(UUID.randomUUID(), orderId, productId, reason, attempts, registeredAt);
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getProductId() { return productId; }
    public String getReason() { return reason; }
    public int getAttempts() { return attempts; }
    public Instant getRegisteredAt() { return registeredAt; }
}
