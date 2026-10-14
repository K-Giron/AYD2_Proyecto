-- Secuencias independientes para el folio de entrada y de salida; el
-- formato humano (ENT-000001/SAL-000001) se construye en FolioGenerator.
CREATE SEQUENCE seq_folio_entrada START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_folio_salida START WITH 1 INCREMENT BY 1;

CREATE TABLE stock_movements (
    id              UUID PRIMARY KEY,
    folio           VARCHAR(16) NOT NULL,
    movement_type   VARCHAR(16) NOT NULL,
    article_id      UUID NOT NULL REFERENCES articles (id),
    quantity        INTEGER NOT NULL,
    reason          VARCHAR(255),
    destinatario    VARCHAR(160),
    occurred_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_stock_movements_folio UNIQUE (folio),
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT ck_stock_movements_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_stock_movements_article_id ON stock_movements (article_id, occurred_at DESC);
