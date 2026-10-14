-- Módulo de logística: un envío por cada folio de salida registrado.
CREATE TABLE shipments (
    id              UUID PRIMARY KEY,
    folio           VARCHAR(16) NOT NULL,
    movement_id     UUID NOT NULL REFERENCES stock_movements (id),
    destinatario    VARCHAR(160),
    status          VARCHAR(16) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_shipments_folio UNIQUE (folio),
    CONSTRAINT ck_shipments_status CHECK (status IN ('PENDIENTE', 'EN_TRANSITO', 'ENTREGADO'))
);
