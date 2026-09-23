package com.megastore.sgologic.payments;

/**
 * Estado del intento de pago (CDU-03, postcondición): aprobado, rechazado o
 * pendiente de confirmación. {@code INDETERMINATE} queda reservado para
 * PAY-02 (timeout, circuit breaker y conciliación).
 */
public enum PaymentAttemptStatus {
    PENDING,
    APPROVED,
    REJECTED,
    INDETERMINATE
}
