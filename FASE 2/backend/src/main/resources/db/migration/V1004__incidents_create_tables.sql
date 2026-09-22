-- Módulo: incidents (Kevin Girón)
-- Tablas propias de INC-01. La venta original se referencia solo por
-- order_id; no hay llave foránea hacia tablas de Orders.

CREATE TABLE incidents (
    id             UUID PRIMARY KEY,
    order_id       UUID NOT NULL,
    type           VARCHAR(16) NOT NULL,
    reason         VARCHAR(500) NOT NULL,
    partial        BOOLEAN NOT NULL DEFAULT FALSE,
    status         VARCHAR(16) NOT NULL,
    refund_amount  NUMERIC(12, 2),
    correlation_id UUID NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    resolved_at    TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_incidents_order_id ON incidents (order_id);

CREATE TABLE incident_items (
    incident_id UUID NOT NULL REFERENCES incidents (id),
    sku         VARCHAR(64) NOT NULL,
    quantity    INTEGER NOT NULL
);

CREATE INDEX ix_incident_items_incident_id ON incident_items (incident_id);
