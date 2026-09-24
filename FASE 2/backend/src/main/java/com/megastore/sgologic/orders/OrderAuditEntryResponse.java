package com.megastore.sgologic.orders;

import java.time.Instant;
import java.util.UUID;

public record OrderAuditEntryResponse(
        UUID id,
        UUID orderId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        String actor,
        String reason,
        UUID correlationId,
        Instant occurredAt
) {
    static OrderAuditEntryResponse from(OrderStatusHistory history) {
        return new OrderAuditEntryResponse(history.getId(), history.getOrderId(),
                history.getPreviousStatus(), history.getNewStatus(), history.getActor(),
                history.getReason(), history.getCorrelationId(), history.getOccurredAt());
    }
}
