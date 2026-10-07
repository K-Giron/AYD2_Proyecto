-- Modulo: integration (ERP-01/ERP-02), ampliado por Kevin Girón (ADR-015).
-- El conector ERP consolida las ventas de todos los canales: la venta web
-- (orden del monolito) y la venta de la tienda física (factura de tienda
-- recibida por el almacén central). order_id sigue siendo la llave única
-- de idempotencia (id de la orden o id de la factura de tienda).
ALTER TABLE erp_sales
    ADD COLUMN channel          VARCHAR(16) NOT NULL DEFAULT 'WEB',
    ADD COLUMN point_of_sale    VARCHAR(32),
    ADD COLUMN source_reference VARCHAR(64);

CREATE INDEX ix_erp_sales_confirmed ON erp_sales (confirmed_at);
