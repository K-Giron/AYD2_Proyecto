-- Módulo: onlinestore (Kevin Girón, ADR-015)
-- La tienda online (POS-ONLINE del almacén central) recibe mercadería por
-- folio igual que una tienda física y publica en la web solo lo que su
-- administrador decide, con imagen, precio y descripción.

-- Ubicación propia de la tienda online dentro del inventario del monolito
-- (módulo inventory, Jorge): lo que se recibe por folio entra aquí, y la
-- reserva atómica, el temporizador de liberación y la confirmación de la
-- Saga operan sobre este stock — el mismo que la web vende. Se siembra
-- desde esta migración porque nace con la tienda online; el módulo
-- inventory no cambia su esquema.
INSERT INTO inventory_locations (id, code, name, type, created_at)
VALUES ('c0000000-0000-0000-0000-00000000000e', 'CD-WEB', 'Almacén Tienda Online (POS-ONLINE)',
        'DISTRIBUTION_CENTER', now())
ON CONFLICT (code) DO NOTHING;

-- Lo que el central envió a POS-ONLINE (sus folios POS-ENT). Llega
-- EN_TRANSITO; solo entra al inventario cuando alguien confirma el folio.
CREATE TABLE online_store_receipts (
    folio           VARCHAR(32)    PRIMARY KEY,
    shipment_folio  VARCHAR(32)    NOT NULL,
    origin          VARCHAR(32)    NOT NULL,
    product_id      UUID           NOT NULL,
    sku             VARCHAR(64)    NOT NULL,
    name            VARCHAR(160)   NOT NULL,
    description     VARCHAR(500),
    price           NUMERIC(12, 2) NOT NULL,
    quantity        INTEGER        NOT NULL CHECK (quantity > 0),
    status          VARCHAR(16)    NOT NULL CHECK (status IN ('EN_TRANSITO', 'RECIBIDO')),
    occurred_at     TIMESTAMPTZ    NOT NULL,
    received_by     VARCHAR(160),
    received_at     TIMESTAMPTZ
);

CREATE INDEX ix_online_store_receipts_shipment ON online_store_receipts (shipment_folio);
CREATE INDEX ix_online_store_receipts_status ON online_store_receipts (status);

-- Ficha que la web muestra. product_id = id del artículo en el almacén
-- central (el mismo con el que se reserva y se reporta la venta). Se crea
-- sin publicar al recibir el artículo por primera vez.
CREATE TABLE online_catalog_products (
    product_id    UUID           PRIMARY KEY,
    sku           VARCHAR(64)    NOT NULL UNIQUE,
    name          VARCHAR(160)   NOT NULL,
    description   VARCHAR(1000),
    category      VARCHAR(80),
    price         NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    published     BOOLEAN        NOT NULL DEFAULT FALSE,
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL
);

-- Imagen aparte de la ficha para no cargar los bytes al listar el catálogo.
CREATE TABLE online_catalog_images (
    product_id    UUID         PRIMARY KEY REFERENCES online_catalog_products (product_id) ON DELETE CASCADE,
    content_type  VARCHAR(40)  NOT NULL,
    data          BYTEA        NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);
