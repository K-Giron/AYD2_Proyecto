package com.megastore.inventoryservice.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * {@code pointOfSaleId} es obligatorio para SALIDA (destino) e ignorado
 * para ENTRADA y AJUSTE. AJUSTE exige {@code reason}. {@code externalReference} es opcional, para quien llama
 * desde afuera (ej. una integración): si se repite, se devuelve el mismo
 * movimiento ya registrado en vez de duplicar el efecto — ver
 * {@code StockService#registerMovement}.
 */
public record RegisterStockMovementRequest(
        @NotNull UUID articleId,
        @NotNull StockMovementType type,
        @Min(1) int quantity,
        @Size(max = 255) String reason,
        UUID pointOfSaleId,
        @Size(max = 160) String externalReference
) {
}
