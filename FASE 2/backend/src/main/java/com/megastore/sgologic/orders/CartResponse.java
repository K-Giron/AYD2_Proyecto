package com.megastore.sgologic.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        CartStatus status,
        SalesChannel originChannel,
        SalesChannel lastChannel,
        List<CartItemResponse> items,
        int itemCount,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {
    static CartResponse from(Cart cart) {
        return new CartResponse(cart.getId(), cart.getCustomerId(), cart.getStatus(),
                cart.getOriginChannel(), cart.getLastChannel(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.itemCount(), cart.total(), cart.getCreatedAt(), cart.getUpdatedAt());
    }
}
