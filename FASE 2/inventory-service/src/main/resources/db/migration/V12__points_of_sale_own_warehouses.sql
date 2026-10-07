-- Cada punto de venta debe tener su propio almacén, no compartir el
-- almacén central con los demás. ALM-CENTRAL se conserva como el único
-- almacén de origen de artículos/existencias/movimientos (ENT-/SAL-), pero
-- ya no queda asignado a ningún punto de venta.
INSERT INTO warehouses (id, code, name) VALUES
    ('a1000000-0000-0000-0000-000000000002', 'ALM-POS-01', 'Almacén Tienda Centro'),
    ('a1000000-0000-0000-0000-000000000003', 'ALM-POS-02', 'Almacén Tienda Zona 10'),
    ('a1000000-0000-0000-0000-000000000004', 'ALM-POS-03', 'Almacén Tienda Mixco');

UPDATE points_of_sale SET warehouse_id = 'a1000000-0000-0000-0000-000000000002'
    WHERE id = 'a2000000-0000-0000-0000-000000000001';
UPDATE points_of_sale SET warehouse_id = 'a1000000-0000-0000-0000-000000000003'
    WHERE id = 'a2000000-0000-0000-0000-000000000002';
UPDATE points_of_sale SET warehouse_id = 'a1000000-0000-0000-0000-000000000004'
    WHERE id = 'a2000000-0000-0000-0000-000000000003';
