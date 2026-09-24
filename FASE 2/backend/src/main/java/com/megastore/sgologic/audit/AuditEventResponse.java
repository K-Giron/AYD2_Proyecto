package com.megastore.sgologic.audit;

import java.time.Instant;
import java.util.UUID;

/** Evidencia auditable expuesta por la consulta de AUD-02 (CDU-12). */
record AuditEventResponse(
        UUID id,
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        UUID orderId,
        String actor,
        String action,
        String result,
        String beforeState,
        String afterState,
        UUID correlationId,
        UUID causationId,
        Instant occurredAt
) {

    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getEventId(), event.getAggregateType(),
                event.getAggregateId(), event.getOrderId(), event.getActor(), event.getAction(),
                event.getResult(), event.getBeforeState(), event.getAfterState(), event.getCorrelationId(),
                event.getCausationId(), event.getOccurredAt());
    }
}
