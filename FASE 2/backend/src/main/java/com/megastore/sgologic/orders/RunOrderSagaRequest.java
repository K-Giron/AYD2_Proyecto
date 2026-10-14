package com.megastore.sgologic.orders;

import com.megastore.sgologic.payments.api.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

/** Datos estables que permiten iniciar o reanudar ORD-03 idempotentemente. */
public record RunOrderSagaRequest(
        PaymentRoute paymentRoute,
        PaymentMethod paymentMethod,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        String paymentReference,
        @NotBlank String taxIdentifier,
        @NotBlank String customerName,
        @NotBlank String destination
) {
    public RunOrderSagaRequest {
        if (paymentRoute == null) paymentRoute = PaymentRoute.PAYKLIK;
    }

    @AssertTrue(message = "La información de pago no corresponde a la ruta seleccionada")
    public boolean isPaymentDataValid() {
        if (paymentRoute == PaymentRoute.PAYKLIK) {
            return paymentMethod != null && paymentReference != null && !paymentReference.isBlank();
        }
        return paymentMethod == null && (paymentReference == null || paymentReference.isBlank());
    }
}
