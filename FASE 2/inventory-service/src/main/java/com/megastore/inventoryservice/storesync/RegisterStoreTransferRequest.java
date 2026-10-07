package com.megastore.inventoryservice.storesync;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** {@code destination}: "CENTRAL" o el código de otro punto de venta. */
public record RegisterStoreTransferRequest(
        @NotBlank @Size(max = 24) String localFolio,
        @NotNull UUID articleId,
        @Min(1) int quantity,
        @NotBlank @Size(max = 32) String destination,
        @Size(max = 255) String reason,
        @NotBlank @Size(max = 80) String seller
) {
}
