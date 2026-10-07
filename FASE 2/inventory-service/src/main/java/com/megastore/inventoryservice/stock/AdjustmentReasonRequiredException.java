package com.megastore.inventoryservice.stock;

public class AdjustmentReasonRequiredException extends RuntimeException {

    public AdjustmentReasonRequiredException() {
        super("Un ajuste de existencia requiere el motivo (merma, daño, corrección de conteo…).");
    }
}
