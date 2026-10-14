package com.megastore.sgologic.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de lectura de CDU-09: vista consolidada de una orden (compra, pago,
 * despacho, entrega e incidencia) sin que el solicitante consulte cada área
 * por separado. Cada campo conserva su propio {@code ...AsOf} para poder
 * mostrar la última información conocida marcándola como posiblemente
 * desactualizada (CDU-09, E1) en vez de fallar toda la consulta.
 *
 * <p>Este módulo no lee tablas de Orders, Payments, Fiscal ni Incidents. Los
 * estados de Orders llegan mediante el sobre de eventos v1 y un Inbox
 * idempotente; la ingesta HTTP se conserva temporalmente para las fuentes que
 * todavía no publican eventos.</p>
 */
@Entity
@Table(name = "order_summary_projections")
public class OrderSummaryProjection {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "order_status", length = 64)
    private String orderStatus;

    @Column(name = "order_status_as_of")
    private Instant orderStatusAsOf;

    @Column(name = "payment_status", length = 64)
    private String paymentStatus;

    @Column(name = "payment_status_as_of")
    private Instant paymentStatusAsOf;

    @Column(name = "dispatch_status", length = 64)
    private String dispatchStatus;

    @Column(name = "dispatch_status_as_of")
    private Instant dispatchStatusAsOf;

    @Column(name = "delivery_status", length = 64)
    private String deliveryStatus;

    @Column(name = "delivery_status_as_of")
    private Instant deliveryStatusAsOf;

    @Column(name = "incident_status", length = 64)
    private String incidentStatus;

    @Column(name = "incident_status_as_of")
    private Instant incidentStatusAsOf;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderSummaryProjection() {
        // Requerido por JPA.
    }

    static OrderSummaryProjection blank(UUID orderId, Instant now) {
        OrderSummaryProjection projection = new OrderSummaryProjection();
        projection.orderId = orderId;
        projection.updatedAt = now;
        return projection;
    }

    /**
     * Aplica el hecho de una fuente. Los eventos pueden llegar desordenados
     * (colas distintas): uno más viejo que el ya registrado no lo reemplaza.
     */
    void apply(ProjectionSource source, String status, Instant asOf, Instant now) {
        Instant current = switch (source) {
            case ORDER -> orderStatusAsOf;
            case PAYMENT -> paymentStatusAsOf;
            case DISPATCH -> dispatchStatusAsOf;
            case DELIVERY -> deliveryStatusAsOf;
            case INCIDENT -> incidentStatusAsOf;
        };
        if (current != null && asOf != null && asOf.isBefore(current)) {
            return;
        }
        switch (source) {
            case ORDER -> {
                this.orderStatus = status;
                this.orderStatusAsOf = asOf;
            }
            case PAYMENT -> {
                this.paymentStatus = status;
                this.paymentStatusAsOf = asOf;
            }
            case DISPATCH -> {
                this.dispatchStatus = status;
                this.dispatchStatusAsOf = asOf;
            }
            case DELIVERY -> {
                this.deliveryStatus = status;
                this.deliveryStatusAsOf = asOf;
            }
            case INCIDENT -> {
                this.incidentStatus = status;
                this.incidentStatusAsOf = asOf;
            }
        }
        this.updatedAt = now;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public Instant getOrderStatusAsOf() {
        return orderStatusAsOf;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public Instant getPaymentStatusAsOf() {
        return paymentStatusAsOf;
    }

    public String getDispatchStatus() {
        return dispatchStatus;
    }

    public Instant getDispatchStatusAsOf() {
        return dispatchStatusAsOf;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public Instant getDeliveryStatusAsOf() {
        return deliveryStatusAsOf;
    }

    public String getIncidentStatus() {
        return incidentStatus;
    }

    public Instant getIncidentStatusAsOf() {
        return incidentStatusAsOf;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
