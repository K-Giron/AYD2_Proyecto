package com.megastore.sgologic.credit.api;

/**
 * Ciclo de una operación de crédito:
 * {@code GRANTED -> SETTLED | OVERDUE | CANCELLED} y {@code OVERDUE -> SETTLED};
 * {@code REJECTED} es terminal desde el inicio.
 */
public enum CreditOperationStatus {
    /** Concedido y vigente: el cliente aún debe pagar. */
    GRANTED,
    /** Rechazado: no genera obligación ni vencimiento. */
    REJECTED,
    /** Plazo de 30 días vencido sin pago; bloquea nuevas concesiones. */
    OVERDUE,
    /** Pagado por el cliente; libera el límite consumido. */
    SETTLED,
    /** Anulado porque la orden se canceló; libera el límite consumido. */
    CANCELLED
}
