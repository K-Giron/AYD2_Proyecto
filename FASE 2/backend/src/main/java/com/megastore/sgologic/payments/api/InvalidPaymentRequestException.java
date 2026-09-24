package com.megastore.sgologic.payments.api;

/**
 * La solicitud de pago no cumple el contrato del puerto (canal/método
 * desconocido, importe inválido o referencia insegura). El mensaje nunca
 * incluye PAN ni CVV.
 *
 * <p>Es una excepción tipada de la API pública para que el adaptador REST
 * responda 400 sin enmascarar otros {@code IllegalArgumentException} de
 * programación.</p>
 */
public class InvalidPaymentRequestException extends RuntimeException {

    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}
