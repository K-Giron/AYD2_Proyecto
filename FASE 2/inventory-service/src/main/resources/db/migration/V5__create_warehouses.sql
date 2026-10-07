CREATE TABLE warehouses (
    id    UUID PRIMARY KEY,
    code  VARCHAR(32) NOT NULL,
    name  VARCHAR(160) NOT NULL,
    CONSTRAINT uq_warehouses_code UNIQUE (code)
);

INSERT INTO warehouses (id, code, name) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'ALM-CENTRAL', 'Almacén Central');
