package com.megastore.sgologic.integration.outbox;

import java.util.UUID;

/**
 * Datos que un módulo propietario entrega para registrar un evento saliente
 * según el sobre común {@code docs/contracts/events/event-envelope-v1.md}.
 * El {@code payload} es específico de cada tipo de evento.
 */
public record OutboxEventRequest(
        String aggregateType,
        UUID aggregateId,
        String eventType,
        int eventVersion,
        UUID correlationId,
        UUID causationId,
        Object payload
) {

    public OutboxEventRequest {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType es obligatorio.");
        }
        if (aggregateId == null) {
            throw new IllegalArgumentException("aggregateId es obligatorio.");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType es obligatorio.");
        }
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion debe ser positivo.");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("correlationId es obligatorio.");
        }
    }

    public static OutboxEventRequest of(String aggregateType, UUID aggregateId, String eventType,
                                        UUID correlationId, Object payload) {
        return new OutboxEventRequest(aggregateType, aggregateId, eventType, 1, correlationId, null, payload);
    }
}
