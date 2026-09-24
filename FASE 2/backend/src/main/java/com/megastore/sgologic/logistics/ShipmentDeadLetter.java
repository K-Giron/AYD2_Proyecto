package com.megastore.sgologic.logistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Cola de mensajes fallidos local a Logistics (LOG-03). Se usa mientras el
 * Outbox transaccional transversal (BB-04, propietario Edson) no está
 * disponible; el sobre de eventos v1 sigue en estado PROPOSED. Cuando el
 * Outbox exista, este registro deja de ser necesario y se sustituye por el
 * mecanismo común.
 */
@Entity
@Table(name = "shipment_dead_letters")
public class ShipmentDeadLetter {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "shipment_id", nullable = false, updatable = false)
    private UUID shipmentId;

    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected ShipmentDeadLetter() {
        // Requerido por JPA.
    }

    private ShipmentDeadLetter(UUID id, UUID shipmentId, String reason, int attempts, Instant registeredAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.reason = reason;
        this.attempts = attempts;
        this.registeredAt = registeredAt;
    }

    public static ShipmentDeadLetter of(UUID shipmentId, String reason, int attempts, Instant registeredAt) {
        return new ShipmentDeadLetter(UUID.randomUUID(), shipmentId, reason, attempts, registeredAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public String getReason() {
        return reason;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
