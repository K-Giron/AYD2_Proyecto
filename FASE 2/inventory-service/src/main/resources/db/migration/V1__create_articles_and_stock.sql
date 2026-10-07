-- Almacén central (inventory-service). Sistema propio, con su propia
-- base de datos.

CREATE TABLE articles (
    id          UUID PRIMARY KEY,
    sku         VARCHAR(64) NOT NULL,
    name        VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    price       NUMERIC(12,2) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_articles_sku UNIQUE (sku),
    CONSTRAINT ck_articles_price_non_negative CHECK (price >= 0)
);

-- Existencia actual por artículo. Solo cambia a través de stock_movements
-- (ver V2); nunca se edita directo.
CREATE TABLE stock (
    article_id        UUID PRIMARY KEY REFERENCES articles (id),
    quantity_on_hand  INTEGER NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ NOT NULL,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_stock_quantity_non_negative CHECK (quantity_on_hand >= 0)
);
