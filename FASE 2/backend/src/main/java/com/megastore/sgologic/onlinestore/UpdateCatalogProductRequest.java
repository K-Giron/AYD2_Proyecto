package com.megastore.sgologic.onlinestore;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateCatalogProductRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description,
        @Size(max = 80) String category,
        @NotNull @DecimalMin("0.01") BigDecimal price
) {
}
