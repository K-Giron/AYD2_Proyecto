package com.megastore.sgologic.orders;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenCartRequest(@NotNull UUID customerId, @NotNull SalesChannel channel) {
}
