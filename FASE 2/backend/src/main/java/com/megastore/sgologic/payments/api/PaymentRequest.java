package com.megastore.sgologic.payments.api;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Solicitud de autorización de pago (CDU-03, paso 2 y 3).
 *
 * <p>{@code paymentReference} es una referencia segura provista por el
 * procesador (token); nunca debe contener PAN completo ni CVV, y el sistema
 * no los almacena (EaC-12).</p>
 */
public record PaymentRequest(
        UUID orderId,
        BigDecimal amount,
        String currency,
        String channel,
        String method,
        String paymentReference,
        UUID correlationId,
        String idempotencyKey
) {

    private static final Pattern CVV_MARKER =
            Pattern.compile("(?i).*(cvv2?|cvc2?|securitycode).*");

    public PaymentRequest {
        if (orderId == null) {
            throw new InvalidPaymentRequestException("orderId es obligatorio.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentRequestException("amount debe ser mayor que cero.");
        }
        if (currency == null || !currency.matches("[A-Za-z]{3}")) {
            throw new InvalidPaymentRequestException("currency debe tener tres letras.");
        }
        if (channel == null || channel.isBlank()) {
            throw new InvalidPaymentRequestException("channel es obligatorio.");
        }
        if (method == null || method.isBlank()) {
            throw new InvalidPaymentRequestException("method es obligatorio.");
        }
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new InvalidPaymentRequestException("paymentReference es obligatoria.");
        }
        if (paymentReference.length() > 128) {
            throw new InvalidPaymentRequestException("paymentReference no puede superar 128 caracteres.");
        }
        // Se normalizan separadores (espacios, guiones, puntos) para detectar
        // PAN formateados como 4111-1111-1111-1111, y se rechazan variantes
        // de CVV/CVC (cvv, cvv2, cvc, cvc2, securityCode).
        String normalizedReference = paymentReference.replaceAll("[\\s\\-.]", "");
        if (normalizedReference.matches("\\d{13,}") || CVV_MARKER.matcher(normalizedReference).matches()) {
            throw new InvalidPaymentRequestException("paymentReference debe ser una referencia tokenizada segura.");
        }
        if (correlationId == null) {
            throw new InvalidPaymentRequestException("correlationId es obligatorio.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidPaymentRequestException("idempotencyKey es obligatoria.");
        }
        if (idempotencyKey.length() > 128) {
            throw new InvalidPaymentRequestException("idempotencyKey no puede superar 128 caracteres.");
        }
    }
}
