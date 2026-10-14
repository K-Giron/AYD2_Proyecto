package com.megastore.sgologic.credit.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditRequest(
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        UUID correlationId,
        String idempotencyKey
) {
}
