package com.megastore.sgologic.credit.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditEvaluationRequest(
        UUID customerId,
        BigDecimal amount,
        String currency,
        UUID correlationId
) {
}
