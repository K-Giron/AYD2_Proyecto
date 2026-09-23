package com.megastore.sgologic.payments;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload del evento {@code PaymentIndeterminate} (PAY-02): permite que la
 * conciliación de PAY-03 y la Saga reaccionen sin leer tablas de payments.
 * No contiene PAN ni CVV.
 */
record PaymentIndeterminatePayload(
        UUID paymentAttemptId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String channel,
        String method,
        String failureCode
) {
}
