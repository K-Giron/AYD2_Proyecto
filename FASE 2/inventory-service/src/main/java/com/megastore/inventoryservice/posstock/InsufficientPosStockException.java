package com.megastore.inventoryservice.posstock;

import java.util.UUID;

public class InsufficientPosStockException extends RuntimeException {

    public InsufficientPosStockException(UUID pointOfSaleId, UUID articleId, int requestedQuantity) {
        super("El punto de venta " + pointOfSaleId + " no tiene existencia suficiente del artículo " + articleId
                + " para registrar una venta de " + requestedQuantity + " unidad(es)");
    }
}
