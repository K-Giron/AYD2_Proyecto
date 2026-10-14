-- Eventos de stock que el almacén central envía a la tienda online
-- (POS-ONLINE): existencia que el central registra tras cada entrada, venta
-- o traslado. Idempotente por evento; cada uno queda en la bitácora.
CREATE TABLE online_store_stock_events (
    event_id    UUID PRIMARY KEY,
    folio       VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE online_store_sync_log DROP CONSTRAINT IF EXISTS online_store_sync_log_kind_check;
ALTER TABLE online_store_sync_log ADD CONSTRAINT online_store_sync_log_kind_check
    CHECK (kind IN ('RECEPCION', 'VENTA', 'TRASLADO', 'EVENTO_STOCK'));
