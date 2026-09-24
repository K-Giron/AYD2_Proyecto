package com.megastore.sgologic.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Evidencia persistida de una entrada o salida de inventario fuera del
 * flujo de reserva de venta en línea (recepción de mercadería, merma,
 * ajuste de tienda/POS). Independiente del saldo actual en
 * {@link InventoryItem}, que solo guarda el total vigente.
 */
@Entity
@Table(name = "inventory_stock_movements")
public class InventoryStockMovement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, updatable = false, length = 16)
    private StockMovementType type;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "reason", updatable = false, length = 255)
    private String reason;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected InventoryStockMovement() {
        // Requerido por JPA.
    }

    static InventoryStockMovement register(UUID productId, UUID locationId, StockMovementType type, int quantity,
                                            String reason, UUID correlationId, Instant occurredAt) {
        InventoryStockMovement movement = new InventoryStockMovement();
        movement.id = UUID.randomUUID();
        movement.productId = productId;
        movement.locationId = locationId;
        movement.type = type;
        movement.quantity = quantity;
        movement.reason = reason;
        movement.correlationId = correlationId;
        movement.occurredAt = occurredAt;
        return movement;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public StockMovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
