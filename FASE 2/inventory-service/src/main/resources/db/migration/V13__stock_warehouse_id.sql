-- Formaliza con una FK real que el stock del catálogo (y cada movimiento
-- de entrada/salida) vive en el almacén central — antes era solo una
-- convención (el primer almacén sembrado), sin ninguna columna que lo
-- respaldara.
ALTER TABLE stock ADD COLUMN warehouse_id UUID;
UPDATE stock SET warehouse_id = 'a1000000-0000-0000-0000-000000000001';
ALTER TABLE stock ALTER COLUMN warehouse_id SET NOT NULL;
ALTER TABLE stock
    ADD CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id);

ALTER TABLE stock_movements ADD COLUMN warehouse_id UUID;
UPDATE stock_movements SET warehouse_id = 'a1000000-0000-0000-0000-000000000001';
ALTER TABLE stock_movements ALTER COLUMN warehouse_id SET NOT NULL;
ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id);
