package com.megastore.sgologic.credit.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreditOperationResult(
        UUID creditOperationId,
        UUID decisionId,
        CreditOperationStatus status,
        BigDecimal approvedLimit,
        LocalDate dueDate,
        String ruleVersion,
        String publicReasonCode,
        Instant processedAt
) {
}
