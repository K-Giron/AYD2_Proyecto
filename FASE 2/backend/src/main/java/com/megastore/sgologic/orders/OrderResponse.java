package com.megastore.sgologic.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID cartId,
        UUID customerId,
        SalesChannel channel,
        OrderStatus status,
        PaymentRoute paymentRoute,
        UUID financialOperationId,
        List<OrderLineResponse> items,
        BigDecimal total,
        UUID correlationId,
        Instant createdAt,
        Instant updatedAt
) {
    static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(order.getId(), order.getCartId(), order.getCustomerId(),
                order.getChannel(), order.getStatus(), order.getPaymentRoute(), order.getFinancialOperationId(),
                order.getItems().stream().map(OrderLineResponse::from).toList(),
                order.getTotal(), order.getCorrelationId(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
