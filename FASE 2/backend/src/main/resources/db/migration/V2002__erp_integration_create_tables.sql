-- Modulo: integration (Andy Sanchez) - ERP-01/ERP-02.

CREATE TABLE erp_batches (
    id               UUID PRIMARY KEY,
    format           VARCHAR(8) NOT NULL,
    payload          TEXT,
    status           VARCHAR(24) NOT NULL,
    sale_count       INTEGER NOT NULL,
    attempts         INTEGER NOT NULL DEFAULT 0,
    acknowledgement  VARCHAR(128),
    failure_message  VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    delivered_at     TIMESTAMPTZ,
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_erp_batches_status_created ON erp_batches (status, created_at);

CREATE TABLE erp_sales (
    id            UUID PRIMARY KEY,
    order_id      UUID NOT NULL,
    net_total     NUMERIC(14,2) NOT NULL,
    tax_total     NUMERIC(14,2) NOT NULL,
    grand_total   NUMERIC(14,2) NOT NULL,
    currency      VARCHAR(3) NOT NULL,
    confirmed_at  TIMESTAMPTZ NOT NULL,
    status        VARCHAR(16) NOT NULL,
    batch_id      UUID REFERENCES erp_batches (id),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_erp_sales_order UNIQUE (order_id),
    CONSTRAINT ck_erp_sales_totals CHECK (
        net_total >= 0 AND tax_total >= 0 AND grand_total > 0
    )
);

CREATE INDEX ix_erp_sales_pending ON erp_sales (status, confirmed_at);
CREATE INDEX ix_erp_sales_batch ON erp_sales (batch_id);
