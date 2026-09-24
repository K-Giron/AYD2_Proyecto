package com.megastore.sgologic.orders;

import com.megastore.sgologic.payments.api.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ProcessPaymentRequest(
        @NotNull PaymentMethod method,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotBlank String paymentReference
) {
}
