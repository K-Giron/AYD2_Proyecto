package com.megastore.sgologic.fiscal;

import java.math.BigDecimal;
import java.util.UUID;

public record FiscalProcessingRequest(
        UUID orderId,
        UUID financialOperationId,
        FinancialOperationType financialOperationType,
        String taxIdentifier,
        String customerName,
        BigDecimal total,
        String currency,
        UUID correlationId,
        String idempotencyKey
) {
}
