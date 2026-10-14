-- Módulo: inventory
-- Registro de entradas y salidas de inventario (recepción de mercadería,
-- mermas, ajustes de tienda/POS) independiente del flujo de reserva de
-- venta en línea (inventory_reservations). Cada movimiento queda como
-- evidencia auditable separada del saldo actual en inventory_items.

CREATE TABLE inventory_stock_movements (
    id              UUID PRIMARY KEY,
    product_id      UUID NOT NULL,
    location_id     UUID NOT NULL REFERENCES inventory_locations (id),
    movement_type   VARCHAR(16) NOT NULL,
    quantity        INTEGER NOT NULL,
    reason          VARCHAR(255),
    correlation_id  UUID NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_inventory_stock_movements_type CHECK (movement_type IN ('INBOUND', 'OUTBOUND')),
    CONSTRAINT ck_inventory_stock_movements_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_inventory_stock_movements_location_id ON inventory_stock_movements (location_id, occurred_at DESC);
CREATE INDEX ix_inventory_stock_movements_product_id ON inventory_stock_movements (product_id, occurred_at DESC);
