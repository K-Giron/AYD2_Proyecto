package com.megastore.sgologic.credit.api;

public enum CreditHistoryType {
    /** Compra completada del cliente. */
    PURCHASE,
    /** Pago de una obligación; {@code onTime} indica si llegó dentro del plazo. */
    PAYMENT,
    /** Devolución asociada a una compra. */
    RETURN,
    /** Incidencia; {@code critical} indica si es de severidad crítica. */
    INCIDENT
}
