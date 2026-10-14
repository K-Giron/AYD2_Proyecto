package com.megastore.sgologic.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Resolución local {@code orderId -> customerId}, alimentada por
 * {@code ORDER_STATUS_CHANGED}. Los eventos de despacho (Logistics) e
 * incidencia (Incidents) no conocen el cliente de la orden -por diseño, no
 * leen tablas de Orders-, así que Notifications recuerda la asociación la
 * primera vez que ve un evento de orden para poder resolver el contacto en
 * eventos posteriores de otros agregados de la misma orden.
 */
@Entity
@Table(name = "order_customer_links")
class OrderCustomerLink {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderCustomerLink() {
        // Requerido por JPA.
    }

    static OrderCustomerLink of(UUID orderId, UUID customerId, Instant now) {
        OrderCustomerLink link = new OrderCustomerLink();
        link.orderId = orderId;
        link.customerId = customerId;
        link.updatedAt = now;
        return link;
    }

    UUID getCustomerId() {
        return customerId;
    }
}
