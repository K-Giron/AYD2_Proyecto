package com.megastore.sgologic.audit;

import java.util.Map;
import java.util.UUID;

public record AuditCommand(
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
        Map<String, Object> metadata
) {
}
