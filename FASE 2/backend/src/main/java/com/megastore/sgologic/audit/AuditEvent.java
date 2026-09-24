package com.megastore.sgologic.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Evidencia auditable de solo anexado (CDU-12). La aplicación no expone
 * operaciones de actualización ni de eliminación sobre esta entidad.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "actor", nullable = false, updatable = false, length = 100)
    private String actor;

    @Column(name = "action", nullable = false, updatable = false, length = 100)
    private String action;

    @Column(name = "result", nullable = false, updatable = false, length = 64)
    private String result;

    @Column(name = "before_state", updatable = false, columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", updatable = false, columnDefinition = "TEXT")
    private String afterState;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "causation_id", updatable = false)
    private UUID causationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
        // Requerido por JPA.
    }

    static AuditEvent create(UUID id, UUID eventId, String aggregateType, UUID aggregateId, UUID orderId,
                             String actor, String action, String result, String beforeState, String afterState,
                             UUID correlationId, UUID causationId, Instant occurredAt) {
        AuditEvent event = new AuditEvent();
        event.id = id;
        event.eventId = eventId == null ? UUID.randomUUID() : eventId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.orderId = orderId;
        event.actor = actor;
        event.action = action;
        event.result = result;
        event.beforeState = beforeState;
        event.afterState = afterState;
        event.correlationId = correlationId;
        event.causationId = causationId;
        event.occurredAt = occurredAt;
        return event;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public UUID getOrderId() { return orderId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getResult() { return result; }
    public String getBeforeState() { return beforeState; }
    public String getAfterState() { return afterState; }
    public UUID getCorrelationId() { return correlationId; }
    public UUID getCausationId() { return causationId; }
    public Instant getOccurredAt() { return occurredAt; }
}
