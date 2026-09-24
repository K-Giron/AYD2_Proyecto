package com.megastore.sgologic.payments;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProcessingRequest(
        UUID orderId,
        String channel,
        PaymentMethod method,
        BigDecimal amount,
        String currency,
        String paymentReference,
        UUID correlationId,
        String idempotencyKey
) {
}
