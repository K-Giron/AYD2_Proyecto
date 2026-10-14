package com.megastore.storeservice.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * {@code taxId} vacío o "CF" = Consumidor Final. {@code webCustomer} y
 * {@code webCartId} solo vienen si el cliente quiso identificarse y se
 * trajo su carrito web.
 */
public record CreateSaleRequest(
        @NotBlank @Size(max = 80) String seller,
        @Size(max = 20) String taxId,
        @Size(max = 160) String customerName,
        @NotNull PaymentMethod paymentMethod,
        @DecimalMin("0.00") BigDecimal cashReceived,
        @NotEmpty List<@Valid Line> lines,
        @Size(max = 160) String webCustomer,
        UUID webCartId
) {
    public record Line(@NotNull UUID articleId, @Min(1) int quantity) {
    }
}
