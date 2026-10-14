package com.megastore.inventoryservice.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        InvoiceType type,
        String counterparty,
        String description,
        BigDecimal amount,
        String relatedFolio,
        LocalDate dueDate,
        InvoiceStatus status,
        Instant createdAt,
        Instant paidAt
) {
    static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getType(), invoice.getCounterparty(),
                invoice.getDescription(), invoice.getAmount(), invoice.getRelatedFolio(), invoice.getDueDate(),
                invoice.getStatus(), invoice.getCreatedAt(), invoice.getPaidAt());
    }
}
