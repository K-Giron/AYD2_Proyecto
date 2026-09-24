package com.megastore.sgologic.incidents;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record IncidentLineItemDto(@NotBlank String sku, @Min(1) int quantity) {

    IncidentLineItem toDomain() {
        return new IncidentLineItem(sku, quantity);
    }

    static IncidentLineItemDto from(IncidentLineItem item) {
        return new IncidentLineItemDto(item.getSku(), item.getQuantity());
    }
}
