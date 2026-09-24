package com.megastore.sgologic.messaging;

import java.util.Map;
import java.util.UUID;

public record OutboxCommand(
        String aggregateType,
        UUID aggregateId,
        String eventType,
        Map<String, Object> payload,
        UUID correlationId,
        String idempotencyKey
) {
}
