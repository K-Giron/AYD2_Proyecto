package com.megastore.sgologic.incidents;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agregado propietario de INC-01 (CDU-10). Referencia la venta original
 * únicamente por {@code orderId}: Incidents no lee tablas de Orders,
 * Payments ni Fiscal. La validación de elegibilidad automática (CDU-10,
 * paso 2, usando historial de pago/despacho/entrega) queda pendiente hasta
 * que AUD-01 y QRY-01 estén integrables por evento; mientras tanto la
 * resolución es manual (E1).
 */
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    /**
     * Opcional: lo entrega quien registra la incidencia (el cliente conoce
     * su propio identificador desde la sesión autenticada). Permite el
     * historial de atenciones por cliente sin que Incidents lea tablas de
     * Orders para resolverlo.
     */
    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private IncidentType type;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "partial", nullable = false)
    private boolean partial;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_items", joinColumns = @JoinColumn(name = "incident_id"))
    private List<IncidentLineItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private IncidentStatus status;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Incident() {
        // Requerido por JPA.
    }

    private Incident(UUID id, UUID orderId, UUID customerId, IncidentType type, String reason, boolean partial,
                      List<IncidentLineItem> items, UUID correlationId, Instant now) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.type = type;
        this.reason = reason;
        this.partial = partial;
        this.items = new ArrayList<>(items);
        this.status = IncidentStatus.REGISTERED;
        this.correlationId = correlationId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Incident register(UUID orderId, UUID customerId, IncidentType type, String reason,
                                     boolean partial, List<IncidentLineItem> items, UUID correlationId, Instant now) {
        return new Incident(UUID.randomUUID(), orderId, customerId, type, reason, partial, items, correlationId, now);
    }

    public void resolve(IncidentStatus resolution, BigDecimal refundAmount, Instant now) {
        this.status = resolution;
        this.refundAmount = refundAmount;
        this.resolvedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public IncidentType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public boolean isPartial() {
        return partial;
    }

    public List<IncidentLineItem> getItems() {
        return List.copyOf(items);
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
