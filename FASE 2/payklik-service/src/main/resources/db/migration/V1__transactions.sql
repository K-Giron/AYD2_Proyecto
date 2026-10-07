-- PayKlik: autorizaciones de cobro de los comercios afiliados.
CREATE SEQUENCE seq_authorization_code START WITH 1;

CREATE TABLE transactions (
    id                 UUID PRIMARY KEY,
    merchant           VARCHAR(80)    NOT NULL,
    -- Llave del comercio: el mismo cobro repetido nunca se cobra dos veces.
    idempotency_key    VARCHAR(120)   NOT NULL,
    order_reference    VARCHAR(80)    NOT NULL,
    amount             NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    currency           VARCHAR(3)     NOT NULL,
    method             VARCHAR(20)    NOT NULL,
    -- Token de tarjeta o referencia de transferencia; nunca PAN ni CVV.
    payment_reference  VARCHAR(120)   NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    authorization_code VARCHAR(20),
    rejection_code     VARCHAR(40),
    created_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_transactions_idempotency UNIQUE (merchant, idempotency_key)
);
CREATE INDEX idx_transactions_created ON transactions (created_at DESC);

CREATE TABLE operation_settings (
    id              SMALLINT PRIMARY KEY CHECK (id = 1),
    mode            VARCHAR(30) NOT NULL,
    slow_latency_ms INTEGER     NOT NULL,
    updated_by      VARCHAR(80) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
INSERT INTO operation_settings (id, mode, slow_latency_ms, updated_by, updated_at)
VALUES (1, 'NORMAL', 7000, 'instalacion', now());
