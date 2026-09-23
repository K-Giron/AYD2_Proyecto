package com.megastore.sgologic.integration.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Evento saliente registrado en la misma transacción que el cambio de
 * negocio (ADR-002). El dispatcher lo publica después en RabbitMQ.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false)
    private int eventVersion;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "causation_id", updatable = false)
    private UUID causationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxEventStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected OutboxEvent() {
        // Requerido por JPA.
    }

    static OutboxEvent create(UUID eventId, String aggregateType, UUID aggregateId, String eventType,
                              int eventVersion, String payload, UUID correlationId, UUID causationId,
                              Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.eventId = eventId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.eventVersion = eventVersion;
        event.payload = payload;
        event.correlationId = correlationId;
        event.causationId = causationId;
        event.status = OutboxEventStatus.PENDING;
        event.attempts = 0;
        event.nextAttemptAt = now;
        event.createdAt = now;
        event.updatedAt = now;
        return event;
    }

    /**
     * Marca el intento de publicación. Devuelve {@code true} si debe
     * reintentarse más tarde con el siguiente backoff; {@code false} cuando
     * se agotaron los intentos y el evento pasa a {@code FAILED}.
     */
    boolean registerFailure(String reason, Instant now, List<Duration> retryDelays, int maxAttempts) {
        attempts += 1;
        failureMessage = reason == null ? null : reason.substring(0, Math.min(reason.length(), 500));
        updatedAt = now;
        if (attempts < maxAttempts) {
            Duration delay = retryDelays.get(Math.min(attempts - 1, retryDelays.size() - 1));
            status = OutboxEventStatus.PENDING;
            nextAttemptAt = now.plus(delay);
            return true;
        }
        status = OutboxEventStatus.FAILED;
        return false;
    }

    void markPublished(Instant now) {
        status = OutboxEventStatus.PUBLISHED;
        publishedAt = now;
        updatedAt = now;
        failureMessage = null;
    }

    public UUID getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public int getEventVersion() { return eventVersion; }
    public String getPayload() { return payload; }
    public UUID getCorrelationId() { return correlationId; }
    public UUID getCausationId() { return causationId; }
    public OutboxEventStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
}
