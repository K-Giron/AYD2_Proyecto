package com.megastore.sgologic.notifications;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull NotificationEventType eventType,
        @NotNull NotificationChannel channel,
        @NotBlank String recipient,
        @NotNull UUID sourceAggregateId
) {
}
