package com.megastore.sgologic.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_id", nullable = false, updatable = false, length = 160)
    private String actorId;

    @Column(name = "actor_type", nullable = false, updatable = false, length = 40)
    private String actorType;

    @Column(nullable = false, updatable = false, length = 120)
    private String action;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> previousState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state", updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> newState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 24)
    private AuditResult result;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(name = "evidence_hash", nullable = false, updatable = false, length = 64)
    private String evidenceHash;

    protected AuditEvent() {
    }

    static AuditEvent from(AuditCommand command, Instant occurredAt, String evidenceHash) {
        AuditEvent event = new AuditEvent();
        event.id = UUID.randomUUID();
        event.actorId = command.actorId();
        event.actorType = command.actorType();
        event.action = command.action();
        event.aggregateType = command.aggregateType();
        event.aggregateId = command.aggregateId();
        event.orderId = command.orderId();
        event.transactionId = command.transactionId();
        event.correlationId = command.correlationId();
        event.previousState = command.previousState() == null ? null : new LinkedHashMap<>(command.previousState());
        event.newState = command.newState() == null ? null : new LinkedHashMap<>(command.newState());
        event.result = command.result();
        event.occurredAt = occurredAt;
        event.metadata = command.metadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(command.metadata());
        event.evidenceHash = evidenceHash;
        return event;
    }

    public UUID getId() { return id; }
    public String getActorId() { return actorId; }
    public String getActorType() { return actorType; }
    public String getAction() { return action; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public UUID getOrderId() { return orderId; }
    public UUID getTransactionId() { return transactionId; }
    public UUID getCorrelationId() { return correlationId; }
    public Map<String, Object> getPreviousState() { return previousState == null ? null : Map.copyOf(previousState); }
    public Map<String, Object> getNewState() { return newState == null ? null : Map.copyOf(newState); }
    public AuditResult getResult() { return result; }
    public Instant getOccurredAt() { return occurredAt; }
    public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }
    public String getEvidenceHash() { return evidenceHash; }
}
