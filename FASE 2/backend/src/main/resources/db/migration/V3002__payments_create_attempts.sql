-- Módulo: payments. CDU-03, EaC-04, EaC-07 y EaC-12.
-- Solo persiste una referencia tokenizada; el modelo no incluye PAN ni CVV.
CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY, order_id UUID NOT NULL, channel VARCHAR(24) NOT NULL,
    method VARCHAR(24) NOT NULL, amount NUMERIC(12,2) NOT NULL, currency VARCHAR(3) NOT NULL,
    payment_reference VARCHAR(180) NOT NULL, status VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(120), result_code VARCHAR(80), result_message VARCHAR(500),
    idempotency_key VARCHAR(128) NOT NULL, correlation_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, processed_at TIMESTAMPTZ, reconciliation_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_attempts_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_payment_attempts_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payment_attempts_channel CHECK (channel IN ('WEB','MOBILE','STORE','CALL_CENTER')),
    CONSTRAINT ck_payment_attempts_method CHECK (method IN ('CARD','TRANSFER','DIGITAL_WALLET')),
    CONSTRAINT ck_payment_attempts_status CHECK (status IN ('PENDING','APPROVED','REJECTED','INDETERMINATE'))
);
CREATE INDEX ix_payment_attempts_order_created ON payment_attempts (order_id, created_at DESC);
CREATE INDEX ix_payment_attempts_reconciliation ON payment_attempts (status, reconciliation_at)
    WHERE status = 'INDETERMINATE';
