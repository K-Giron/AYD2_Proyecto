-- Módulo: query (Kevin Girón)
-- Modelo de lectura consolidado de QRY-01. Se alimenta por API mientras el
-- Outbox transversal (BB-04) y el consumidor de eventos no existen.

CREATE TABLE order_summary_projections (
    order_id               UUID PRIMARY KEY,
    order_status           VARCHAR(64),
    order_status_as_of     TIMESTAMPTZ,
    payment_status         VARCHAR(64),
    payment_status_as_of   TIMESTAMPTZ,
    dispatch_status        VARCHAR(64),
    dispatch_status_as_of  TIMESTAMPTZ,
    delivery_status        VARCHAR(64),
    delivery_status_as_of  TIMESTAMPTZ,
    incident_status        VARCHAR(64),
    incident_status_as_of  TIMESTAMPTZ,
    updated_at             TIMESTAMPTZ NOT NULL
);
