package com.megastore.sgologic.payments;

/** Resultado interno del comando: el intento y si fue creado en esta llamada. */
record PaymentAttemptOutcome(PaymentAttempt attempt, boolean created) {
}
