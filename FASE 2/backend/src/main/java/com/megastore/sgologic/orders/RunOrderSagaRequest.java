package com.megastore.sgologic.orders;

import com.megastore.sgologic.payments.api.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Datos estables que permiten iniciar o reanudar ORD-03 idempotentemente. */
public record RunOrderSagaRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotBlank String paymentReference,
        @NotBlank String taxIdentifier,
        @NotBlank String customerName,
        @NotBlank String destination
) {
}
