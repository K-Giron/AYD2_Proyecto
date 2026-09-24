package com.megastore.sgologic.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Contrato público v1 de los eventos publicados por el Outbox. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessEventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        UUID correlationId,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
