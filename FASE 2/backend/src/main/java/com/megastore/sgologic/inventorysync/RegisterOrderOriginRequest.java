package com.megastore.sgologic.inventorysync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterOrderOriginRequest(@NotNull UUID orderId, @NotBlank @Size(max = 32) String pointOfSaleCode) {
}
