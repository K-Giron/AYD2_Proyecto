-- Punto de venta (tienda): tiene su propio identificador y un almacén
-- asignado del que recibe mercadería.
CREATE TABLE points_of_sale (
    id            UUID PRIMARY KEY,
    code          VARCHAR(32) NOT NULL,
    name          VARCHAR(160) NOT NULL,
    warehouse_id  UUID NOT NULL REFERENCES warehouses (id),
    CONSTRAINT uq_points_of_sale_code UNIQUE (code)
);

INSERT INTO points_of_sale (id, code, name, warehouse_id) VALUES
    ('a2000000-0000-0000-0000-000000000001', 'POS-01', 'Tienda Centro', 'a1000000-0000-0000-0000-000000000001'),
    ('a2000000-0000-0000-0000-000000000002', 'POS-02', 'Tienda Zona 10', 'a1000000-0000-0000-0000-000000000001'),
    ('a2000000-0000-0000-0000-000000000003', 'POS-03', 'Tienda Mixco', 'a1000000-0000-0000-0000-000000000001');
