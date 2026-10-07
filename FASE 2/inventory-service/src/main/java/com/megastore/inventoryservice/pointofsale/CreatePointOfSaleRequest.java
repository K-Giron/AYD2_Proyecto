package com.megastore.inventoryservice.pointofsale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePointOfSaleRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 160) String name
) {
}
