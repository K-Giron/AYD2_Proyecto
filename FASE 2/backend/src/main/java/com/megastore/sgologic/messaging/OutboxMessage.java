package com.megastore.sgologic.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 120)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 180)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Version
    @Column(nullable = false)
    private long version;

    protected OutboxMessage() {
    }

    private OutboxMessage(OutboxCommand command, Instant now) {
        this.id = UUID.randomUUID();
        this.aggregateType = command.aggregateType();
        this.aggregateId = command.aggregateId();
        this.eventType = command.eventType();
        this.payload = new LinkedHashMap<>(command.payload());
        this.correlationId = command.correlationId();
        this.idempotencyKey = command.idempotencyKey();
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.availableAt = now;
        this.createdAt = now;
    }

    static OutboxMessage pending(OutboxCommand command, Instant now) {
        return new OutboxMessage(command, now);
    }

    void markPublished(Instant now) {
        status = OutboxStatus.PUBLISHED;
        publishedAt = now;
        lastError = null;
    }

    void registerFailure(String message, Instant nextAttempt, int maximumAttempts) {
        attempts++;
        lastError = message == null ? "Error de publicación sin detalle" : message.substring(0, Math.min(500, message.length()));
        availableAt = nextAttempt;
        if (attempts >= maximumAttempts) status = OutboxStatus.DEAD_LETTER;
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public Map<String, Object> getPayload() {
        // Los eventos de creación expresan previousStatus=null; Map.copyOf
        // rechaza valores nulos y detenía el drenaje completo del Outbox.
        return Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
    public UUID getCorrelationId() { return correlationId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public OutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getAvailableAt() { return availableAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getLastError() { return lastError; }
}
