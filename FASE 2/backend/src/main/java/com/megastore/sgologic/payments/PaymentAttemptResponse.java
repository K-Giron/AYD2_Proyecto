package com.megastore.sgologic.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(
        UUID id,
        UUID orderId,
        String channel,
        PaymentMethod method,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String providerReference,
        String resultCode,
        String resultMessage,
        UUID correlationId,
        Instant createdAt,
        Instant processedAt,
        Instant reconciliationAt
) {
    static PaymentAttemptResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(attempt.getId(), attempt.getOrderId(), attempt.getChannel(),
                attempt.getMethod(), attempt.getAmount(), attempt.getCurrency(), attempt.getStatus(),
                attempt.getProviderReference(), attempt.getResultCode(), attempt.getResultMessage(),
                attempt.getCorrelationId(), attempt.getCreatedAt(), attempt.getProcessedAt(),
                attempt.getReconciliationAt());
    }
}
