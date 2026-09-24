package com.megastore.sgologic.query;

import java.util.UUID;

public class OrderSummaryNotFoundException extends RuntimeException {

    public OrderSummaryNotFoundException(UUID orderId) {
        super("No hay información consolidada para la orden " + orderId);
    }
}
