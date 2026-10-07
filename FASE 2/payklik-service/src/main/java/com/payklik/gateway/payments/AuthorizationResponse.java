package com.payklik.gateway.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code approved} y {@code status} (APROBADA/RECHAZADA) dicen lo mismo; el primero es para integraciones. */
public record AuthorizationResponse(UUID transactionId, String idempotencyKey, String orderReference, BigDecimal amount,
                                    String currency, String method, String paymentReference, boolean approved,
                                    String status, String authorizationCode, String rejectionCode, Instant processedAt) {

    static AuthorizationResponse of(PaymentTransaction tx) {
        return new AuthorizationResponse(tx.getId(), tx.getIdempotencyKey(), tx.getOrderReference(), tx.getAmount(),
                tx.getCurrency(), tx.getMethod(), tx.getPaymentReference(),
                PaymentTransaction.APPROVED.equals(tx.getStatus()), tx.getStatus(), tx.getAuthorizationCode(),
                tx.getRejectionCode(), tx.getCreatedAt());
    }
}
