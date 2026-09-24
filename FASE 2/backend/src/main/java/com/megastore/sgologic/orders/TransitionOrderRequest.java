package com.megastore.sgologic.orders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionOrderRequest(
        @NotNull OrderStatus status,
        @NotBlank @Size(max = 120) String actor,
        @Size(max = 500) String reason
) {
}
