package com.megastore.sgologic.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentOptionsResponse(
        UUID cartId,
        String currency,
        BigDecimal total,
        Instant evaluatedAt,
        String ruleVersion,
        List<PaymentOptionResponse> options
) {
}
