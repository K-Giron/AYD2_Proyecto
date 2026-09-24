package com.megastore.sgologic.payments.api;

import java.math.BigDecimal;
import java.util.UUID;

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

    public PaymentRequest {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId es obligatorio.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero.");
        }
        if (currency == null || !currency.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("currency debe tener tres letras.");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel es obligatorio.");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method es obligatorio.");
        }
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("paymentReference es obligatoria.");
        }
        if (paymentReference.matches(".*\\d{13,}.*") || paymentReference.toLowerCase().contains("cvv")) {
            throw new IllegalArgumentException("paymentReference contiene datos sensibles no permitidos.");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("correlationId es obligatorio.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey es obligatoria.");
        }
    }
}
