-- Módulo: notifications (Kevin Girón)
-- Tabla propia de NOT-01. Correlaciona por source_aggregate_id (p. ej.
-- orderId o shipmentId) sin leer tablas de otros módulos.

CREATE TABLE notification_requests (
    id                   UUID PRIMARY KEY,
    event_type           VARCHAR(32) NOT NULL,
    channel              VARCHAR(16) NOT NULL,
    recipient            VARCHAR(200) NOT NULL,
    source_aggregate_id  UUID NOT NULL,
    correlation_id       UUID NOT NULL,
    status               VARCHAR(16) NOT NULL,
    attempts             INTEGER NOT NULL DEFAULT 0,
    last_error           VARCHAR(300),
    created_at           TIMESTAMPTZ NOT NULL,
    sent_at              TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_notification_requests_source_aggregate_id ON notification_requests (source_aggregate_id);
