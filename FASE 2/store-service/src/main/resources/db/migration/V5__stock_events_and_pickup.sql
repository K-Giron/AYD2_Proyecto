-- Eventos de stock que el almacén central envía a esta tienda (existencia
-- que el central tiene registrada después de cada entrada, venta o traslado)
-- y entrega al cliente de los pedidos web que LogiExpress deja aquí.
CREATE TABLE stock_events (
    event_id    UUID PRIMARY KEY,
    folio       VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE sync_log DROP CONSTRAINT IF EXISTS sync_log_kind_check;
ALTER TABLE sync_log ADD CONSTRAINT sync_log_kind_check
    CHECK (kind IN ('FACTURA', 'TRASLADO', 'RECEPCION', 'CARRITO_WEB', 'EVENTO_STOCK', 'ENTREGA_PEDIDO'));
