package com.megastore.inventoryservice.logistics;

import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(@NotNull ShipmentStatus status) {
}
