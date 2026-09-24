package com.megastore.sgologic.payments;

import java.time.Instant;
import java.util.UUID;

public record PaymentProcessingResult(
        UUID attemptId,
        UUID orderId,
        PaymentStatus status,
        String providerReference,
        String resultCode,
        Instant processedAt
) {
}
