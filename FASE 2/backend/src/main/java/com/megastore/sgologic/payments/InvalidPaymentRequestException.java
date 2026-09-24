package com.megastore.sgologic.payments;

/**
 * La solicitud de pago no cumple el contrato (canal/método desconocido o
 * datos inválidos). Nunca incluye PAN ni CVV en el mensaje.
 */
class InvalidPaymentRequestException extends RuntimeException {

    InvalidPaymentRequestException(String message) {
        super(message);
    }
}
