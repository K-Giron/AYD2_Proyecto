package com.megastore.sgologic.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String actorId,
        String actorType,
        String action,
        String aggregateType,
        UUID aggregateId,
        UUID orderId,
        UUID transactionId,
        UUID correlationId,
        Map<String, Object> previousState,
        Map<String, Object> newState,
        AuditResult result,
        Instant occurredAt,
        Map<String, Object> metadata,
        String evidenceHash
) {
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getActorId(), event.getActorType(),
                event.getAction(), event.getAggregateType(), event.getAggregateId(), event.getOrderId(),
                event.getTransactionId(), event.getCorrelationId(), event.getPreviousState(),
                event.getNewState(), event.getResult(), event.getOccurredAt(), event.getMetadata(),
                event.getEvidenceHash());
    }
}
