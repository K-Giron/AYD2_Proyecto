-- Integración con los sistemas de tienda (store-service): facturas que
-- cada tienda sube después de vender, y traslados por folio desde una
-- tienda hacia el almacén central o hacia otra tienda.
ALTER TABLE pos_stock_movements DROP CONSTRAINT ck_pos_stock_movements_type;
ALTER TABLE pos_stock_movements
    ADD CONSTRAINT ck_pos_stock_movements_type CHECK (movement_type IN ('ENTRADA', 'VENTA', 'TRASLADO'));

CREATE SEQUENCE seq_store_invoice START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_store_transfer START WITH 1 INCREMENT BY 1;

CREATE TABLE store_invoices (
    id                UUID PRIMARY KEY,
    central_folio     VARCHAR(16)  NOT NULL,
    point_of_sale_id  UUID         NOT NULL REFERENCES points_of_sale (id),
    point_of_sale_code VARCHAR(32) NOT NULL,
    ticket_folio      VARCHAR(24)  NOT NULL,
    issued_at         TIMESTAMPTZ  NOT NULL,
    received_at       TIMESTAMPTZ  NOT NULL,
    seller            VARCHAR(80)  NOT NULL,
    tax_id            VARCHAR(20)  NOT NULL,
    customer_name     VARCHAR(160) NOT NULL,
    payment_method    VARCHAR(24)  NOT NULL,
    total             NUMERIC(12, 2) NOT NULL,
    CONSTRAINT uq_store_invoices_central_folio UNIQUE (central_folio),
    CONSTRAINT uq_store_invoices_ticket UNIQUE (point_of_sale_code, ticket_folio)
);

CREATE TABLE store_invoice_lines (
    invoice_id  UUID NOT NULL REFERENCES store_invoices (id) ON DELETE CASCADE,
    article_id  UUID NOT NULL,
    sku         VARCHAR(64)  NOT NULL,
    name        VARCHAR(160) NOT NULL,
    unit_price  NUMERIC(12, 2) NOT NULL,
    quantity    INTEGER NOT NULL,
    line_total  NUMERIC(12, 2) NOT NULL
);

CREATE TABLE store_transfers (
    id                   UUID PRIMARY KEY,
    central_folio        VARCHAR(16)  NOT NULL,
    origin_pos_id        UUID         NOT NULL REFERENCES points_of_sale (id),
    origin_pos_code      VARCHAR(32)  NOT NULL,
    local_folio          VARCHAR(24)  NOT NULL,
    article_id           UUID         NOT NULL REFERENCES articles (id),
    quantity             INTEGER      NOT NULL,
    destination          VARCHAR(32)  NOT NULL,
    destination_folio    VARCHAR(24),
    reason               VARCHAR(255),
    seller               VARCHAR(80)  NOT NULL,
    occurred_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_store_transfers_central_folio UNIQUE (central_folio),
    CONSTRAINT uq_store_transfers_local UNIQUE (origin_pos_code, local_folio),
    CONSTRAINT ck_store_transfers_quantity CHECK (quantity > 0)
);
