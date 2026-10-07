package com.megastore.sgologic.inventorysync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tienda física donde se cobró una orden. Sin registro aquí, la orden es
 * una venta web (va a "Tienda Online"). Se guarda en este módulo y no en
 * {@code orders} para no tocar el modelo de órdenes.
 */
@Entity
@Table(name = "inventorysync_order_origins")
public class OrderOrigin {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "point_of_sale_code", nullable = false, updatable = false, length = 32)
    private String pointOfSaleCode;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected OrderOrigin() {
        // Requerido por JPA.
    }

    static OrderOrigin of(UUID orderId, String pointOfSaleCode, Instant now) {
        OrderOrigin origin = new OrderOrigin();
        origin.orderId = orderId;
        origin.pointOfSaleCode = pointOfSaleCode;
        origin.registeredAt = now;
        return origin;
    }

    public UUID getOrderId() { return orderId; }
    public String getPointOfSaleCode() { return pointOfSaleCode; }
    public Instant getRegisteredAt() { return registeredAt; }
}
