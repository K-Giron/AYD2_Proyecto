package com.megastore.sgologic.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
    @Id
    private UUID id;
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 40)
    private OrderStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 40)
    private OrderStatus newStatus;
    @Column(nullable = false, length = 120)
    private String actor;
    @Column(length = 500)
    private String reason;
    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OrderStatusHistory() {
    }

    private OrderStatusHistory(UUID orderId, OrderStatus previousStatus, OrderStatus newStatus,
                               String actor, String reason, UUID correlationId, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actor = actor;
        this.reason = reason;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    static OrderStatusHistory of(UUID orderId, OrderStatus previousStatus, OrderStatus newStatus,
                                 String actor, String reason, UUID correlationId, Instant occurredAt) {
        return new OrderStatusHistory(orderId, previousStatus, newStatus, actor, reason, correlationId, occurredAt);
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public OrderStatus getPreviousStatus() { return previousStatus; }
    public OrderStatus getNewStatus() { return newStatus; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
}
