-- Módulo: payments (Edson Sosa) - PAY-01.
-- Intentos de pago conforme a CDU-03 (pasos 2 y 3), ADR-003 y EaC-12:
-- conserva orden, canal, fecha, referencia y resultado sin PAN completo ni
-- CVV. La conciliación (payment_reconciliations) pertenece a PAY-03 y no se
-- crea en esta migración. Rango V3001-V3999 ratificado para
-- integration/payments/audit.

CREATE TABLE payment_attempts (
    id                  UUID PRIMARY KEY,
    order_id            UUID NOT NULL,
    channel             VARCHAR(32) NOT NULL,
    method              VARCHAR(32) NOT NULL,
    amount              NUMERIC(14,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    status              VARCHAR(24) NOT NULL,
    provider_reference  VARCHAR(100),
    provider_attempt_id UUID,
    failure_code        VARCHAR(64),
    failure_message     VARCHAR(500),
    idempotency_key     VARCHAR(128) NOT NULL,
    correlation_id      UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_attempts_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_payment_attempts_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payment_attempts_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'INDETERMINATE'))
);

CREATE INDEX ix_payment_attempts_order ON payment_attempts (order_id, created_at DESC);
CREATE INDEX ix_payment_attempts_status ON payment_attempts (status, updated_at);
