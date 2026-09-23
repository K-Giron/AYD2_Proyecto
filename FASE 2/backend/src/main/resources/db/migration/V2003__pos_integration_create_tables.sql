-- Modulo: integration (Andy Sanchez) - POS-01.

CREATE TABLE pos_deliveries (
    id                  UUID PRIMARY KEY,
    event_id            UUID NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    order_id            UUID NOT NULL,
    store_number        INTEGER NOT NULL,
    payload             TEXT NOT NULL,
    status              VARCHAR(24) NOT NULL,
    attempts            INTEGER NOT NULL DEFAULT 0,
    acknowledgement     VARCHAR(160),
    failure_message     VARCHAR(500),
    correlation_id      UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    acknowledged_at     TIMESTAMPTZ,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_pos_delivery_event_store UNIQUE (event_id, store_number),
    CONSTRAINT ck_pos_delivery_store CHECK (store_number BETWEEN 1 AND 32)
);

CREATE INDEX ix_pos_deliveries_event ON pos_deliveries (event_id, store_number);
CREATE INDEX ix_pos_deliveries_status ON pos_deliveries (status, updated_at);
