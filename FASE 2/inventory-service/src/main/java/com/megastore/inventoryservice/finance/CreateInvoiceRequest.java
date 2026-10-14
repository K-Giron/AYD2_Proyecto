package com.megastore.inventoryservice.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvoiceRequest(
        @NotNull InvoiceType type,
        @NotBlank @Size(max = 160) String counterparty,
        @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @Size(max = 16) String relatedFolio,
        @NotNull @FutureOrPresent LocalDate dueDate
) {
}
