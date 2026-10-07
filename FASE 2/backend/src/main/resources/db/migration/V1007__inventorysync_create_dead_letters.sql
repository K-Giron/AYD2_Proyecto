-- Bandeja de ventas web que no se pudieron reflejar en inventory-service
-- (agotados los reintentos, o el nombre del producto no tiene artículo
-- correspondiente). order_id es solo un identificador por valor: este
-- módulo no es dueño de la tabla de órdenes.
CREATE TABLE inventory_bridge_dead_letters (
    id            UUID PRIMARY KEY,
    order_id      UUID NOT NULL,
    product_id    UUID,
    reason        VARCHAR(255) NOT NULL,
    attempts      INTEGER NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_inventory_bridge_dead_letters_order_id ON inventory_bridge_dead_letters (order_id);
