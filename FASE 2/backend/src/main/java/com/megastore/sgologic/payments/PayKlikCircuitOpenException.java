package com.megastore.sgologic.payments;

/**
 * El circuit breaker de PayKlik está abierto: no se llama al proveedor para
 * evitar una falla en cascada (ADR-003). El intento queda indeterminado.
 */
class PayKlikCircuitOpenException extends RuntimeException {

    PayKlikCircuitOpenException(String message) {
        super(message);
    }
}
