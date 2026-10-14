package com.megastore.sgologic.incidents;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        IncidentType type,
        String reason,
        boolean partial,
        List<IncidentLineItemDto> items,
        IncidentStatus status,
        BigDecimal refundAmount,
        UUID correlationId,
        Instant createdAt,
        Instant resolvedAt
) {
    static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getOrderId(),
                incident.getCustomerId(),
                incident.getType(),
                incident.getReason(),
                incident.isPartial(),
                incident.getItems().stream().map(IncidentLineItemDto::from).toList(),
                incident.getStatus(),
                incident.getRefundAmount(),
                incident.getCorrelationId(),
                incident.getCreatedAt(),
                incident.getResolvedAt()
        );
    }
}
