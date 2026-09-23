package com.megastore.sgologic.payments;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload de los eventos de pago {@code PaymentApproved},
 * {@code PaymentRejected} y {@code PaymentIndeterminate}: permite que la
 * Saga y la conciliación reaccionen sin leer tablas de payments. No contiene
 * PAN ni CVV.
 */
record PaymentEventPayload(
        UUID paymentAttemptId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String channel,
        String method,
        String status,
        String providerReference,
        String failureCode
) {
}
