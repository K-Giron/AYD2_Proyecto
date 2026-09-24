package com.megastore.sgologic.orders;

import java.time.Instant;
import java.util.UUID;

public record OrderPaymentResponse(
        OrderResponse order,
        UUID paymentAttemptId,
        String paymentStatus,
        String providerReference,
        Instant processedAt
) {
    static OrderPaymentResponse from(OrderPaymentOutcome outcome) {
        return new OrderPaymentResponse(OrderResponse.from(outcome.order()), outcome.payment().paymentAttemptId(),
                outcome.payment().status(), outcome.payment().providerReference(),
                outcome.payment().processedAt());
    }
}
