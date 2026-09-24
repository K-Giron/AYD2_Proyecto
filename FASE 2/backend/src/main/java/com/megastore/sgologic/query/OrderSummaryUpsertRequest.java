package com.megastore.sgologic.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record OrderSummaryUpsertRequest(
        @NotNull UUID orderId,
        @NotNull ProjectionSource source,
        @NotBlank String status,
        Instant asOf
) {
}
