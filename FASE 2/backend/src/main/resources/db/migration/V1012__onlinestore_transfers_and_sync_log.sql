-- Módulo: onlinestore (Kevin Girón, ADR-015)
-- La tienda online, como cualquier punto de venta, saca mercadería solo por
-- folio (hacia el almacén central o hacia otra tienda) y lleva una bitácora
-- de lo que baja del central y de lo que le reporta.

CREATE SEQUENCE seq_online_transfer START WITH 1 INCREMENT BY 1;

-- PENDIENTE: salió de CD-WEB y aún no llega al central (se reintenta);
-- RECHAZADO: el central no lo aceptó y la mercadería volvió a CD-WEB.
CREATE TABLE online_store_transfers (
    id                 UUID PRIMARY KEY,
    folio              VARCHAR(24)  NOT NULL UNIQUE,
    product_id         UUID         NOT NULL,
    sku                VARCHAR(64)  NOT NULL,
    name               VARCHAR(160) NOT NULL,
    quantity           INTEGER      NOT NULL CHECK (quantity > 0),
    destination        VARCHAR(32)  NOT NULL,
    reason             VARCHAR(255),
    requested_by       VARCHAR(160) NOT NULL,
    sync_status        VARCHAR(16)  NOT NULL CHECK (sync_status IN ('PENDIENTE', 'ENVIADA', 'RECHAZADO')),
    central_folio      VARCHAR(24),
    destination_folio  VARCHAR(32),
    sync_attempts      INTEGER      NOT NULL DEFAULT 0,
    last_sync_error    VARCHAR(500),
    created_at         TIMESTAMPTZ  NOT NULL,
    synced_at          TIMESTAMPTZ
);

CREATE INDEX ix_online_store_transfers_status ON online_store_transfers (sync_status);

-- BAJADA: lo que llega del central (envíos, confirmación de recepción).
-- SUBIDA: lo que la tienda online le reporta (ventas, traslados).
CREATE TABLE online_store_sync_log (
    id           UUID PRIMARY KEY,
    direction    VARCHAR(8)   NOT NULL CHECK (direction IN ('SUBIDA', 'BAJADA')),
    kind         VARCHAR(16)  NOT NULL CHECK (kind IN ('RECEPCION', 'VENTA', 'TRASLADO')),
    reference    VARCHAR(64),
    result       VARCHAR(8)   NOT NULL CHECK (result IN ('OK', 'ERROR')),
    detail       VARCHAR(500) NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_online_store_sync_log_occurred ON online_store_sync_log (occurred_at DESC);
