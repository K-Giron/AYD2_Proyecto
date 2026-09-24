-- Módulo: logistics (Kevin Girón)
-- Tablas propias de LOG-01/LOG-02/LOG-03. No crea ni referencia tablas de
-- otros módulos (orders, inventory, fiscal): la orden se correlaciona
-- únicamente por order_id.

CREATE TABLE shipments (
    id               UUID PRIMARY KEY,
    order_id         UUID NOT NULL,
    origin           VARCHAR(32) NOT NULL,
    destination      VARCHAR(500) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    tracking_number  VARCHAR(64),
    correlation_id   UUID NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    label_attempts   INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_shipments_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX ix_shipments_order_id ON shipments (order_id);

CREATE TABLE shipment_tracking_events (
    id           UUID PRIMARY KEY,
    shipment_id  UUID NOT NULL REFERENCES shipments (id),
    status       VARCHAR(32) NOT NULL,
    source       VARCHAR(32) NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_shipment_tracking_events_shipment_id ON shipment_tracking_events (shipment_id, occurred_at);

-- Cola de fallidos local (LOG-03) mientras el Outbox transversal (BB-04) no
-- está disponible. Ver FASE 2/backend/.../logistics/ShipmentDeadLetter.java.
CREATE TABLE shipment_dead_letters (
    id            UUID PRIMARY KEY,
    shipment_id   UUID NOT NULL REFERENCES shipments (id),
    reason        VARCHAR(200) NOT NULL,
    attempts      INTEGER NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL
);
