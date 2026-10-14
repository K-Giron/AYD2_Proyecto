package com.megastore.sgologic.credit.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditEvaluationResult(
        UUID decisionId,
        boolean eligible,
        BigDecimal approvedLimit,
        String ruleVersion,
        String publicReasonCode,
        int termDays,
        Instant evaluatedAt
) {
}
