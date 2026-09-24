package com.megastore.sgologic.orders;

import com.megastore.sgologic.inventory.InventoryReservationAllocation;

import java.util.List;

public record InventoryReservationOutcome(
        CustomerOrder order,
        List<InventoryReservationAllocation> allocations
) {
}
