package com.megastore.sgologic.logistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Agregado propietario del despacho de una orden (CDU-07). Referencia la
 * orden únicamente por {@code orderId}: Logistics no lee ni escribe tablas
 * de Orders, Inventory ni Fiscal.
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 32)
    private DispatchOrigin origin;

    /** Almacén concreto del que sale (ALM-CENTRAL, ALM-POS-01, CD-WEB…); nulo si no se indicó. */
    @Column(name = "origin_code", length = 32)
    private String originCode;

    @Column(name = "destination", nullable = false, length = 500)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status;

    @Column(name = "tracking_number", length = 64)
    private String trackingNumber;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "label_attempts", nullable = false)
    private int labelAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Shipment() {
        // Requerido por JPA.
    }

    private Shipment(UUID id, UUID orderId, DispatchOrigin origin, String destination,
                      UUID correlationId, String idempotencyKey, Instant now) {
        this.id = id;
        this.orderId = orderId;
        this.origin = origin;
        this.destination = destination;
        this.status = origin == DispatchOrigin.CUSTOMER_PICKUP
                ? ShipmentStatus.READY_FOR_PICKUP
                : ShipmentStatus.ASSIGNED;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.labelAttempts = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Shipment assign(UUID orderId, DispatchOrigin origin, String destination,
                                   UUID correlationId, String idempotencyKey, Instant now) {
        return new Shipment(UUID.randomUUID(), orderId, origin, destination, correlationId, idempotencyKey, now);
    }

    public static Shipment assign(UUID orderId, DispatchOrigin origin, String originCode, String destination,
                                  UUID correlationId, String idempotencyKey, Instant now) {
        Shipment shipment = assign(orderId, origin, destination, correlationId, idempotencyKey, now);
        shipment.originCode = originCode == null || originCode.isBlank() ? null : originCode.trim();
        return shipment;
    }

    public boolean requiresExternalLabel() {
        return origin != DispatchOrigin.CUSTOMER_PICKUP;
    }

    public void markLabelRequested(Instant now) {
        this.status = ShipmentStatus.LABEL_REQUESTED;
        this.labelAttempts += 1;
        this.updatedAt = now;
    }

    public void markLabelIssued(String trackingNumber, Instant now) {
        this.trackingNumber = trackingNumber;
        this.status = ShipmentStatus.LABEL_ISSUED;
        this.updatedAt = now;
    }

    /** Reintento manual tras agotar los automáticos: el conteo empieza de nuevo. */
    public void reopenLabelRequest(Instant now) {
        this.labelAttempts = 0;
        this.status = ShipmentStatus.LABEL_REQUESTED;
        this.updatedAt = now;
    }

    public void markLabelFailed(Instant now) {
        this.status = ShipmentStatus.LABEL_FAILED;
        this.updatedAt = now;
    }

    public void applyTrackingStatus(ShipmentStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public DispatchOrigin getOrigin() {
        return origin;
    }

    public String getOriginCode() {
        return originCode;
    }

    public String getDestination() {
        return destination;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getLabelAttempts() {
        return labelAttempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
