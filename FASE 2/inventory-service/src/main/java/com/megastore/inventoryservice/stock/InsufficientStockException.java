package com.megastore.inventoryservice.stock;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID articleId, int requestedQuantity) {
        super("No hay existencia suficiente del artículo " + articleId
                + " para registrar una salida de " + requestedQuantity + " unidad(es)");
    }
}
