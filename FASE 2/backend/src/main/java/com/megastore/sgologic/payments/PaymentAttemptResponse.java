package com.megastore.sgologic.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Vista pública del intento de pago (PAY-01). Nunca expone PAN ni CVV. */
record PaymentAttemptResponse(
        UUID id,
        UUID orderId,
        String channel,
        String method,
        BigDecimal amount,
        String currency,
        String status,
        String providerReference,
        UUID providerAttemptId,
        String failureCode,
        String failureMessage,
        UUID correlationId,
        Instant createdAt,
        Instant updatedAt
) {

    static PaymentAttemptResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(attempt.getId(), attempt.getOrderId(),
                attempt.getChannel().name(), attempt.getMethod().name(), attempt.getAmount(),
                attempt.getCurrency(), attempt.getStatus().name(), attempt.getProviderReference(),
                attempt.getProviderAttemptId(), attempt.getFailureCode(), attempt.getFailureMessage(),
                attempt.getCorrelationId(), attempt.getCreatedAt(), attempt.getUpdatedAt());
    }
}
