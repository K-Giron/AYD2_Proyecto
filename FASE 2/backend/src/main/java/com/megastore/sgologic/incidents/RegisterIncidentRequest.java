package com.megastore.sgologic.incidents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RegisterIncidentRequest(
        @NotNull UUID orderId,
        @NotNull IncidentType type,
        @NotBlank String reason,
        boolean partial,
        @NotEmpty List<@Valid IncidentLineItemDto> items
) {
}
