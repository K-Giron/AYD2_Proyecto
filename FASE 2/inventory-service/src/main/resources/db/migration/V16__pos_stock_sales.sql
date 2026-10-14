-- Venta en tienda física: baja la existencia propia del punto de venta
-- (no la del almacén central). Los movimientos del punto de venta dejan
-- de ser solo entradas por folio del almacén.
ALTER TABLE pos_stock_movements ADD COLUMN movement_type VARCHAR(16) NOT NULL DEFAULT 'ENTRADA';
ALTER TABLE pos_stock_movements
    ADD CONSTRAINT ck_pos_stock_movements_type CHECK (movement_type IN ('ENTRADA', 'VENTA'));
ALTER TABLE pos_stock_movements ALTER COLUMN origin_warehouse_folio DROP NOT NULL;
ALTER TABLE pos_stock_movements ADD COLUMN external_reference VARCHAR(160);
ALTER TABLE pos_stock_movements
    ADD CONSTRAINT uq_pos_stock_movements_external_reference UNIQUE (external_reference);

CREATE SEQUENCE seq_pos_folio_venta START WITH 1 INCREMENT BY 1;
