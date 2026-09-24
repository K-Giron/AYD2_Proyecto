package com.megastore.sgologic.orders;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(UUID productId, String productName, BigDecimal unitPrice,
                               int quantity, BigDecimal lineTotal) {
    static CartItemResponse from(CartItem item) {
        return new CartItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.total());
    }
}
