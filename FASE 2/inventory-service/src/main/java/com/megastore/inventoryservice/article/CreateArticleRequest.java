package com.megastore.inventoryservice.article;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateArticleRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price
) {
}
