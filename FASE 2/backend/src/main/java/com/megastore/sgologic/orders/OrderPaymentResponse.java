package com.megastore.sgologic.orders;

import com.megastore.sgologic.payments.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderPaymentResponse(
        OrderResponse order,
        UUID paymentAttemptId,
        PaymentStatus paymentStatus,
        String providerReference,
        String resultCode,
        Instant processedAt
) {
    static OrderPaymentResponse from(OrderPaymentOutcome outcome) {
        return new OrderPaymentResponse(OrderResponse.from(outcome.order()), outcome.payment().attemptId(),
                outcome.payment().status(), outcome.payment().providerReference(),
                outcome.payment().resultCode(), outcome.payment().processedAt());
    }
}
