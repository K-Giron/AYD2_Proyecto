package com.megastore.sgologic.orders;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PutCartItemRequest(
        @NotBlank String productName,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @Min(1) @Max(999) int quantity
) {
}
