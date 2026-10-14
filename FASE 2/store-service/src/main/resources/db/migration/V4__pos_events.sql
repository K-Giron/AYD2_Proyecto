-- Eventos que SGO-Logic emite a este POS (paso 10 del flujo: coherencia de
-- información en las tiendas). Uno por evento; repetirlo no lo duplica.
CREATE TABLE pos_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(40)  NOT NULL,
    order_id     UUID         NOT NULL,
    status       VARCHAR(24)  NOT NULL,
    pickup_store VARCHAR(16),
    payload      TEXT         NOT NULL,
    received_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_pos_events_order ON pos_events (order_id, received_at DESC);
