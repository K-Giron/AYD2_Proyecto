-- Existencia propia de cada punto de venta, separada de la del almacén
-- central. Solo cambia cuando el almacén registra un folio de salida
-- hacia ese punto de venta (ver pos_stock_movements).
CREATE SEQUENCE seq_pos_folio_entrada START WITH 1 INCREMENT BY 1;

CREATE TABLE pos_stock (
    id                UUID PRIMARY KEY,
    point_of_sale_id  UUID NOT NULL REFERENCES points_of_sale (id),
    article_id        UUID NOT NULL REFERENCES articles (id),
    quantity_on_hand  INTEGER NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ NOT NULL,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_pos_stock_point_article UNIQUE (point_of_sale_id, article_id),
    CONSTRAINT ck_pos_stock_quantity_non_negative CHECK (quantity_on_hand >= 0)
);

-- Entrada en el punto de venta, correlacionada con el folio de salida
-- del almacén que la originó.
CREATE TABLE pos_stock_movements (
    id                       UUID PRIMARY KEY,
    folio                    VARCHAR(24) NOT NULL,
    point_of_sale_id         UUID NOT NULL REFERENCES points_of_sale (id),
    article_id               UUID NOT NULL REFERENCES articles (id),
    quantity                 INTEGER NOT NULL,
    origin_warehouse_folio   VARCHAR(16) NOT NULL,
    occurred_at              TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_pos_stock_movements_folio UNIQUE (folio),
    CONSTRAINT ck_pos_stock_movements_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_pos_stock_movements_point_of_sale ON pos_stock_movements (point_of_sale_id, occurred_at DESC);
CREATE INDEX ix_pos_stock_movements_origin_folio ON pos_stock_movements (origin_warehouse_folio);
