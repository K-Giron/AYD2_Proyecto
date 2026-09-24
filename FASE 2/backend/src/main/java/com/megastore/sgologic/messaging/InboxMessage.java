package com.megastore.sgologic.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_messages")
class InboxMessage {

    @EmbeddedId
    private InboxMessageId id;

    @Column(name = "event_type", nullable = false, updatable = false, length = 120)
    private String eventType;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private InboxStatus status;

    protected InboxMessage() {
    }

    static InboxMessage received(String consumerName, BusinessEventEnvelope event, Instant now) {
        InboxMessage message = new InboxMessage();
        message.id = new InboxMessageId(consumerName, event.eventId());
        message.eventType = event.eventType();
        message.correlationId = event.correlationId();
        message.receivedAt = now;
        message.status = InboxStatus.RECEIVED;
        return message;
    }

    void markProcessed(Instant now) {
        status = InboxStatus.PROCESSED;
        processedAt = now;
    }

    InboxStatus getStatus() {
        return status;
    }
}
