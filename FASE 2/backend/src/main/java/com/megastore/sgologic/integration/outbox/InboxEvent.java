package com.megastore.sgologic.integration.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Libro idempotente del consumidor: registra que un evento ya fue procesado
 * por una cola concreta. La restricción única {@code (consumer, event_id)}
 * es la garantía ante reenvíos del broker (semántica al menos una vez).
 */
@Entity
@Table(name = "inbox_event")
public class InboxEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "consumer", nullable = false, updatable = false, length = 100)
    private String consumer;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected InboxEvent() {
        // Requerido por JPA.
    }

    static InboxEvent create(String consumer, UUID eventId, Instant now) {
        InboxEvent event = new InboxEvent();
        event.id = UUID.randomUUID();
        event.consumer = consumer;
        event.eventId = eventId;
        event.processedAt = now;
        return event;
    }

    public UUID getId() { return id; }
    public String getConsumer() { return consumer; }
    public UUID getEventId() { return eventId; }
    public Instant getProcessedAt() { return processedAt; }
}
