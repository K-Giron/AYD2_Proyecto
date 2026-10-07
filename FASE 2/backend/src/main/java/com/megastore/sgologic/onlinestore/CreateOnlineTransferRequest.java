package com.megastore.sgologic.onlinestore;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** {@code destination}: "CENTRAL" o el código de una tienda (POS-01…). */
public record CreateOnlineTransferRequest(
        @NotNull UUID productId,
        @Min(1) int quantity,
        @NotBlank @Size(max = 32) String destination,
        @Size(max = 255) String reason
) {
}
