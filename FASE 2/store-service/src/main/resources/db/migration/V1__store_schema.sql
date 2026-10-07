-- Sistema propio de la tienda física. Su catálogo es solo lo que el almacén
-- central le ha enviado por folio: store_articles usa el mismo id de artículo
-- que el central para que las facturas y traslados se puedan cruzar.

CREATE TABLE store_articles (
    id          UUID PRIMARY KEY,
    sku         VARCHAR(64)    NOT NULL UNIQUE,
    name        VARCHAR(160)   NOT NULL,
    price       NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    updated_at  TIMESTAMPTZ    NOT NULL
);

CREATE TABLE store_stock (
    article_id  UUID PRIMARY KEY REFERENCES store_articles (id),
    quantity    INTEGER     NOT NULL CHECK (quantity >= 0),
    updated_at  TIMESTAMPTZ NOT NULL
);

-- Cada recepción del central (folio POS-ENT) se aplica una sola vez.
CREATE TABLE store_receipts (
    folio         VARCHAR(32) PRIMARY KEY,
    origin_folio  VARCHAR(32),
    article_id    UUID        NOT NULL REFERENCES store_articles (id),
    quantity      INTEGER     NOT NULL CHECK (quantity > 0),
    occurred_at   TIMESTAMPTZ NOT NULL,
    applied_at    TIMESTAMPTZ NOT NULL
);

CREATE SEQUENCE seq_ticket;

CREATE TABLE sales (
    id               UUID PRIMARY KEY,
    folio            VARCHAR(24)    NOT NULL UNIQUE,
    seller           VARCHAR(80)    NOT NULL,
    tax_id           VARCHAR(20)    NOT NULL,
    customer_name    VARCHAR(160)   NOT NULL,
    payment_method   VARCHAR(24)    NOT NULL CHECK (payment_method IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    total            NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    cash_received    NUMERIC(12, 2),
    change_given     NUMERIC(12, 2),
    web_customer     VARCHAR(160),
    web_cart_id      UUID,
    sync_status      VARCHAR(16)    NOT NULL CHECK (sync_status IN ('PENDIENTE', 'ENVIADA', 'ERROR')),
    central_folio    VARCHAR(24),
    sync_attempts    INTEGER        NOT NULL DEFAULT 0,
    last_sync_error  VARCHAR(500),
    created_at       TIMESTAMPTZ    NOT NULL,
    synced_at        TIMESTAMPTZ
);

CREATE INDEX idx_sales_sync_status ON sales (sync_status);

CREATE TABLE sale_lines (
    sale_id      UUID           NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    line_no      INTEGER        NOT NULL,
    article_id   UUID           NOT NULL REFERENCES store_articles (id),
    sku          VARCHAR(64)    NOT NULL,
    name         VARCHAR(160)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    line_total   NUMERIC(12, 2) NOT NULL,
    PRIMARY KEY (sale_id, line_no)
);

CREATE SEQUENCE seq_transfer;

-- Traslado de mercadería de esta tienda hacia el central o hacia otra tienda.
-- RECHAZADO: el central no lo aceptó y la existencia se devolvió a la tienda.
CREATE TABLE transfers (
    id                UUID PRIMARY KEY,
    folio             VARCHAR(24)  NOT NULL UNIQUE,
    article_id        UUID         NOT NULL REFERENCES store_articles (id),
    quantity          INTEGER      NOT NULL CHECK (quantity > 0),
    destination       VARCHAR(32)  NOT NULL,
    reason            VARCHAR(255),
    seller            VARCHAR(80)  NOT NULL,
    sync_status       VARCHAR(16)  NOT NULL CHECK (sync_status IN ('PENDIENTE', 'ENVIADA', 'RECHAZADO')),
    central_folio     VARCHAR(24),
    destination_folio VARCHAR(32),
    sync_attempts     INTEGER      NOT NULL DEFAULT 0,
    last_sync_error   VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL,
    synced_at         TIMESTAMPTZ
);

CREATE INDEX idx_transfers_sync_status ON transfers (sync_status);

-- Bitácora de lo que la tienda envía al central (SUBIDA) y lo que baja (BAJADA).
CREATE TABLE sync_log (
    id           UUID PRIMARY KEY,
    direction    VARCHAR(8)   NOT NULL CHECK (direction IN ('SUBIDA', 'BAJADA')),
    kind         VARCHAR(16)  NOT NULL CHECK (kind IN ('FACTURA', 'TRASLADO', 'RECEPCION', 'CARRITO_WEB')),
    reference    VARCHAR(64),
    result       VARCHAR(8)   NOT NULL CHECK (result IN ('OK', 'ERROR')),
    detail       VARCHAR(500) NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_sync_log_occurred_at ON sync_log (occurred_at DESC);
