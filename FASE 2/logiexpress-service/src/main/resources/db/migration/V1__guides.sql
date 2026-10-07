-- LogiExpress: guías de envío de los comercios afiliados y su rastreo.
CREATE SEQUENCE seq_tracking_number START WITH 1;

CREATE TABLE guides (
    id                 UUID PRIMARY KEY,
    tracking_number    VARCHAR(20)  NOT NULL UNIQUE,
    merchant           VARCHAR(80)  NOT NULL,
    -- Identificador del envío en el sistema del comercio: hace idempotente la solicitud.
    merchant_reference VARCHAR(80)  NOT NULL,
    order_reference    VARCHAR(80),
    origin             VARCHAR(200) NOT NULL,
    destination        VARCHAR(400) NOT NULL,
    -- Tienda donde el cliente recoge (p. ej. POS-01); NULL = entrega a domicilio.
    pickup_point       VARCHAR(20),
    status             VARCHAR(30)  NOT NULL,
    courier            VARCHAR(80),
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_guides_merchant_reference UNIQUE (merchant, merchant_reference)
);
CREATE INDEX idx_guides_merchant_updated ON guides (merchant, updated_at);

CREATE TABLE guide_events (
    id          UUID PRIMARY KEY,
    guide_id    UUID         NOT NULL REFERENCES guides (id),
    status      VARCHAR(30)  NOT NULL,
    note        VARCHAR(400),
    actor       VARCHAR(80)  NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_guide_events_guide ON guide_events (guide_id, occurred_at);

-- Estado operativo del servicio (para mantenimiento y pruebas de contingencia del comercio).
CREATE TABLE operation_settings (
    id              SMALLINT PRIMARY KEY CHECK (id = 1),
    mode            VARCHAR(30) NOT NULL,
    slow_latency_ms INTEGER     NOT NULL,
    updated_by      VARCHAR(80) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
INSERT INTO operation_settings (id, mode, slow_latency_ms, updated_by, updated_at)
VALUES (1, 'NORMAL', 8000, 'instalacion', now());
