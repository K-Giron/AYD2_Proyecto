package com.megastore.sgologic.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Entrada o salida de inventario fuera del flujo de reserva de venta en
 * línea: recepción de mercadería, merma o ajuste de tienda/POS.
 */
public record RegisterStockMovementRequest(
        @NotNull UUID productId,
        @NotNull UUID locationId,
        @NotNull StockMovementType type,
        @Min(1) int quantity,
        @Size(max = 255) String reason
) {
}
