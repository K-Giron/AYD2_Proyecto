package com.megastore.inventoryservice.posstock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** {@code externalReference} vuelve idempotente la venta ante reintentos de quien la registra. */
public record RegisterPosSaleRequest(
        @NotNull UUID pointOfSaleId,
        @NotNull UUID articleId,
        @Min(1) int quantity,
        @Size(max = 255) String reason,
        @Size(max = 160) String externalReference
) {
}
