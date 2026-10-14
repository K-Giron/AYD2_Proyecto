package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Explicación consultable de una obligación de crédito (EDS3-04): reconstruye
 * quién, cuándo, con qué regla, resultado y razón, tanto de la operación como
 * de la decisión que la originó.
 */
public record CreditOperationExplanation(Operation operation, Decision decision) {

    public record Operation(UUID id, UUID orderId, UUID customerId, CreditOperationStatus status,
                            BigDecimal amount, String currency, BigDecimal approvedLimit,
                            LocalDate dueDate, String ruleVersion, String reasonCode,
                            Instant createdAt, UUID correlationId) {
    }

    public record Decision(UUID id, boolean eligible, BigDecimal requestedAmount,
                           BigDecimal approvedLimit, String ruleVersion, String reasonCode,
                           String inputsSnapshot, Instant evaluatedAt, UUID correlationId) {
    }
}
