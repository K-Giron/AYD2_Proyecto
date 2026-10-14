package com.megastore.inventoryservice.storesync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UploadStoreInvoiceRequest(
        @NotBlank @Size(max = 24) String ticketFolio,
        @NotNull Instant issuedAt,
        @NotBlank @Size(max = 80) String seller,
        @NotBlank @Size(max = 20) String taxId,
        @NotBlank @Size(max = 160) String customerName,
        @NotBlank @Size(max = 24) String paymentMethod,
        @NotNull @DecimalMin("0.00") BigDecimal total,
        @NotEmpty List<@Valid Line> lines
) {
    public record Line(
            @NotNull UUID articleId,
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 160) String name,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            @Min(1) int quantity,
            @NotNull @DecimalMin("0.00") BigDecimal lineTotal
    ) {
    }
}
