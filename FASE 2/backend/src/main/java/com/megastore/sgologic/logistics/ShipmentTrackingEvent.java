package com.megastore.sgologic.logistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Historial de estados de entrega comunicados por LogiExpress o generados
 * internamente (CDU-08, paso 4-5: "el proveedor comunica estados
 * posteriores de entrega" y "el sistema registra cada intercambio").
 */
@Entity
@Table(name = "shipment_tracking_events")
public class ShipmentTrackingEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "shipment_id", nullable = false, updatable = false)
    private UUID shipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ShipmentTrackingEvent() {
        // Requerido por JPA.
    }

    private ShipmentTrackingEvent(UUID id, UUID shipmentId, ShipmentStatus status, String source, Instant occurredAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.status = status;
        this.source = source;
        this.occurredAt = occurredAt;
    }

    public static ShipmentTrackingEvent of(UUID shipmentId, ShipmentStatus status, String source, Instant occurredAt) {
        return new ShipmentTrackingEvent(UUID.randomUUID(), shipmentId, status, source, occurredAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
