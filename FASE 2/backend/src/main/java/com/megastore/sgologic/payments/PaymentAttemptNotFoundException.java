package com.megastore.sgologic.payments;

import java.util.UUID;

class PaymentAttemptNotFoundException extends RuntimeException {

    PaymentAttemptNotFoundException(UUID id) {
        super("No existe el intento de pago " + id + ".");
    }
}
