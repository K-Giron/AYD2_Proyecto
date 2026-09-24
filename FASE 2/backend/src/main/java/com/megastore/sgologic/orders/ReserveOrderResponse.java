package com.megastore.sgologic.orders;

import com.megastore.sgologic.inventory.InventoryReservationAllocation;

import java.util.List;

public record ReserveOrderResponse(
        OrderResponse order,
        List<InventoryReservationAllocation> reservations
) {
    static ReserveOrderResponse from(InventoryReservationOutcome outcome) {
        return new ReserveOrderResponse(OrderResponse.from(outcome.order()), outcome.allocations());
    }
}
