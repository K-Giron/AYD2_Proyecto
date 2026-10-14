-- Permite que quien registra un movimiento desde afuera (ej. una
-- integración con otro sistema) marque una referencia propia; un
-- reintento de red con la misma referencia no debe duplicar el efecto
-- sobre la existencia (ver StockService.registerMovement).
ALTER TABLE stock_movements ADD COLUMN external_reference VARCHAR(160);
ALTER TABLE stock_movements
    ADD CONSTRAINT uq_stock_movements_external_reference UNIQUE (external_reference);
