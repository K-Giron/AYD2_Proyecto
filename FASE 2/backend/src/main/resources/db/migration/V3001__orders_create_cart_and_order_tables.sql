-- Módulo: orders. ORD-01/ORD-02 y CDU-01.
CREATE TABLE carts (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL, status VARCHAR(24) NOT NULL,
    origin_channel VARCHAR(24) NOT NULL, last_channel VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL, correlation_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_carts_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_carts_status CHECK (status IN ('ACTIVE','CHECKED_OUT','ABANDONED')),
    CONSTRAINT ck_carts_origin_channel CHECK (origin_channel IN ('WEB','MOBILE','STORE','CALL_CENTER')),
    CONSTRAINT ck_carts_last_channel CHECK (last_channel IN ('WEB','MOBILE','STORE','CALL_CENTER'))
);
CREATE UNIQUE INDEX uq_carts_active_customer ON carts (customer_id) WHERE status = 'ACTIVE';
CREATE INDEX ix_carts_customer_updated_at ON carts (customer_id, updated_at DESC);

CREATE TABLE cart_items (
    cart_id UUID NOT NULL REFERENCES carts (id) ON DELETE CASCADE, product_id UUID NOT NULL,
    product_name VARCHAR(180) NOT NULL, unit_price NUMERIC(12,2) NOT NULL, quantity INTEGER NOT NULL,
    PRIMARY KEY (cart_id, product_id),
    CONSTRAINT ck_cart_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0)
);

CREATE TABLE customer_orders (
    id UUID PRIMARY KEY, cart_id UUID NOT NULL, customer_id UUID NOT NULL, channel VARCHAR(24) NOT NULL,
    status VARCHAR(40) NOT NULL, total NUMERIC(12,2) NOT NULL, checkout_key VARCHAR(128) NOT NULL,
    correlation_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_customer_orders_cart UNIQUE (cart_id),
    CONSTRAINT uq_customer_orders_checkout_key UNIQUE (checkout_key),
    CONSTRAINT ck_customer_orders_total_non_negative CHECK (total >= 0),
    CONSTRAINT ck_customer_orders_channel CHECK (channel IN ('WEB','MOBILE','STORE','CALL_CENTER')),
    CONSTRAINT ck_customer_orders_status CHECK (status IN (
        'CREATED','INVENTORY_RESERVED','PAYMENT_PENDING','PAYMENT_APPROVED','PAYMENT_REJECTED',
        'PAYMENT_INDETERMINATE','FISCAL_PENDING','FISCAL_APPROVED','FISCAL_REJECTED','CONFIRMED',
        'PREPARING','IN_TRANSIT','DELIVERED','CANCELLED'))
);
CREATE INDEX ix_customer_orders_customer_created_at ON customer_orders (customer_id, created_at DESC);
CREATE INDEX ix_customer_orders_status_updated_at ON customer_orders (status, updated_at);

CREATE TABLE customer_order_items (
    order_id UUID NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE, product_id UUID NOT NULL,
    product_name VARCHAR(180) NOT NULL, unit_price NUMERIC(12,2) NOT NULL, quantity INTEGER NOT NULL,
    line_total NUMERIC(12,2) NOT NULL, PRIMARY KEY (order_id, product_id),
    CONSTRAINT ck_customer_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_customer_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_customer_order_items_line_total_non_negative CHECK (line_total >= 0)
);

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE,
    previous_status VARCHAR(40), new_status VARCHAR(40) NOT NULL, actor VARCHAR(120) NOT NULL,
    reason VARCHAR(500), correlation_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_order_status_history_order_occurred ON order_status_history (order_id, occurred_at);
