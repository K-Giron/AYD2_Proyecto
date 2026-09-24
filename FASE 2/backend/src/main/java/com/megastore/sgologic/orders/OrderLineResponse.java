package com.megastore.sgologic.orders;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(UUID productId, String productName, BigDecimal unitPrice,
                                int quantity, BigDecimal lineTotal) {
    static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getProductId(), line.getProductName(), line.getUnitPrice(),
                line.getQuantity(), line.getLineTotal());
    }
}
