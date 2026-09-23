package com.megastore.sgologic.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_deliveries", uniqueConstraints =
        @UniqueConstraint(name = "uq_pos_delivery_event_store", columnNames = {"event_id", "store_number"}))
public class PosDelivery {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "store_number", nullable = false, updatable = false)
    private int storeNumber;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PosDeliveryStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "acknowledgement", length = 160)
    private String acknowledgement;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PosDelivery() {
    }

    static PosDelivery create(UUID eventId, String eventType, UUID orderId, int storeNumber,
                              String payload, UUID correlationId, Instant now) {
        PosDelivery delivery = new PosDelivery();
        delivery.id = UUID.randomUUID();
        delivery.eventId = eventId;
        delivery.eventType = eventType;
        delivery.orderId = orderId;
        delivery.storeNumber = storeNumber;
        delivery.payload = payload;
        delivery.status = PosDeliveryStatus.PENDING;
        delivery.correlationId = correlationId;
        delivery.createdAt = now;
        delivery.updatedAt = now;
        return delivery;
    }

    void markAttempt(Instant now) { attempts += 1; updatedAt = now; }
    void acknowledge(String acknowledgement, Instant now) {
        status = PosDeliveryStatus.ACKNOWLEDGED;
        this.acknowledgement = acknowledgement;
        failureMessage = null;
        acknowledgedAt = now;
        updatedAt = now;
    }
    void reject(String reason, Instant now) {
        status = PosDeliveryStatus.REJECTED;
        failureMessage = reason;
        updatedAt = now;
    }
    void pendingRetry(String reason, Instant now) {
        status = PosDeliveryStatus.PENDING_RETRY;
        failureMessage = reason;
        updatedAt = now;
    }
    void deadLetter(String reason, Instant now) {
        status = PosDeliveryStatus.DEAD_LETTER;
        failureMessage = reason;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getOrderId() { return orderId; }
    public int getStoreNumber() { return storeNumber; }
    public String getPayload() { return payload; }
    public PosDeliveryStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getAcknowledgement() { return acknowledgement; }
    public String getFailureMessage() { return failureMessage; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
}
