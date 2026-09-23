package com.megastore.sgologic.payments;

/**
 * Estado del trabajo de conciliación (PAY-03): pendiente, resuelto o en la
 * bandeja de fallidos tras agotar los reintentos.
 */
public enum PaymentReconciliationStatus {
    PENDING,
    RESOLVED,
    DEAD_LETTER
}
