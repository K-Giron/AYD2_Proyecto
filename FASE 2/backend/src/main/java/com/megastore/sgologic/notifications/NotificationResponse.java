package com.megastore.sgologic.notifications;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationEventType eventType,
        NotificationChannel channel,
        String recipient,
        UUID sourceAggregateId,
        NotificationStatus status,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant sentAt
) {
    static NotificationResponse from(NotificationRequest request) {
        return new NotificationResponse(
                request.getId(),
                request.getEventType(),
                request.getChannel(),
                request.getRecipient(),
                request.getSourceAggregateId(),
                request.getStatus(),
                request.getAttempts(),
                request.getLastError(),
                request.getCreatedAt(),
                request.getSentAt()
        );
    }
}
