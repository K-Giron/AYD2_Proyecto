package com.megastore.sgologic.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitud de notificación (CUN-11). Referencia el hecho de origen solo por
 * {@code sourceAggregateId} (p. ej. {@code orderId} o {@code shipmentId}):
 * Notifications no lee tablas de otros módulos.
 */
@Entity
@Table(name = "notification_requests")
public class NotificationRequest {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "recipient", nullable = false, length = 200)
    private String recipient;

    @Column(name = "source_aggregate_id", nullable = false, updatable = false)
    private UUID sourceAggregateId;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 300)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationRequest() {
        // Requerido por JPA.
    }

    private NotificationRequest(UUID id, NotificationEventType eventType, NotificationChannel channel,
                                 String recipient, UUID sourceAggregateId, UUID correlationId, Instant now) {
        this.id = id;
        this.eventType = eventType;
        this.channel = channel;
        this.recipient = recipient;
        this.sourceAggregateId = sourceAggregateId;
        this.correlationId = correlationId;
        this.status = NotificationStatus.PENDING;
        this.attempts = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static NotificationRequest create(NotificationEventType eventType, NotificationChannel channel,
                                               String recipient, UUID sourceAggregateId, UUID correlationId,
                                               Instant now) {
        return new NotificationRequest(UUID.randomUUID(), eventType, channel, recipient, sourceAggregateId,
                correlationId, now);
    }

    public void markAttempt(Instant now) {
        this.attempts += 1;
        this.updatedAt = now;
    }

    public void markSent(Instant now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        this.status = NotificationStatus.FAILED;
        this.lastError = error;
        this.updatedAt = now;
    }

    public void markPendingRetry(String error, Instant now) {
        this.status = NotificationStatus.PENDING;
        this.lastError = error;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public UUID getSourceAggregateId() {
        return sourceAggregateId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
