-- Eventos de stock hacia el sistema de cada punto de venta (POS local): cada
-- entrada, venta o traslado que cambia la existencia de una tienda queda en
-- esta bandeja en la misma transacción y se entrega con reintentos.
CREATE TABLE pos_stock_events (
    id                UUID PRIMARY KEY,
    point_of_sale_id  UUID         NOT NULL REFERENCES points_of_sale (id),
    pos_code          VARCHAR(20)  NOT NULL,
    article_id        UUID         NOT NULL,
    sku               VARCHAR(64)  NOT NULL,
    article_name      VARCHAR(200) NOT NULL,
    movement          VARCHAR(20)  NOT NULL CHECK (movement IN ('ENTRADA', 'VENTA', 'TRASLADO')),
    delta             INTEGER      NOT NULL,
    quantity_after    INTEGER      NOT NULL,
    folio             VARCHAR(40)  NOT NULL,
    reference         VARCHAR(120),
    -- PENDIENTE → ENTREGADO; REINTENTO tras una falla de red; FALLIDO al
    -- agotar los reintentos; SIN_TERMINAL si el punto de venta aún no tiene
    -- sistema conectado (se vuelve a intentar cada minuto).
    status            VARCHAR(20)  NOT NULL,
    attempts          INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ  NOT NULL,
    last_error        VARCHAR(400),
    acknowledgement   VARCHAR(200),
    created_at        TIMESTAMPTZ  NOT NULL,
    delivered_at      TIMESTAMPTZ
);
CREATE INDEX idx_pos_stock_events_due ON pos_stock_events (status, next_attempt_at);
CREATE INDEX idx_pos_stock_events_pos ON pos_stock_events (pos_code, created_at DESC);
