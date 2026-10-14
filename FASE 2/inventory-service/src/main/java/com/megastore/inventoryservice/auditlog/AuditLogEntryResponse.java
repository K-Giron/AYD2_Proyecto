package com.megastore.inventoryservice.auditlog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id,
        String eventType,
        String description,
        String relatedFolio,
        UUID pointOfSaleId,
        String actor,
        Instant occurredAt
) {
    static AuditLogEntryResponse from(AuditLogEntry entry) {
        return new AuditLogEntryResponse(entry.getId(), entry.getEventType(), entry.getDescription(),
                entry.getRelatedFolio(), entry.getPointOfSaleId(), entry.getActor(), entry.getOccurredAt());
    }
}
