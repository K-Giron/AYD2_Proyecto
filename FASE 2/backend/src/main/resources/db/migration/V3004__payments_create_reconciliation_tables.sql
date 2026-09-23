-- Módulo: payments (Edson Sosa) - PAY-03.
-- Conciliación/reintento idempotente de intentos indeterminados conforme a
-- CDU-03 E1 y ADR-003: el primer trabajo se programa dentro de los 60 s,
-- reutiliza la misma Idempotency-Key (nunca un segundo cobro) y alerta si
-- el caso sigue sin resolver después de cinco minutos.
-- Rango V3001-V3999 ratificado para integration/payments/audit.

-- La conciliación reutiliza la misma solicitud idempotente, así que el
-- intento conserva la referencia segura (nunca PAN ni CVV).
ALTER TABLE payment_attempts ADD COLUMN payment_reference VARCHAR(128);

CREATE TABLE payment_reconciliations (
    id                 UUID PRIMARY KEY,
    payment_attempt_id UUID NOT NULL REFERENCES payment_attempts (id),
    status             VARCHAR(24) NOT NULL,
    result             VARCHAR(64),
    attempts           INTEGER NOT NULL DEFAULT 0,
    failure_code       VARCHAR(64),
    failure_message    VARCHAR(500),
    scheduled_at       TIMESTAMPTZ NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_payment_reconciliations_attempts_non_negative CHECK (attempts >= 0),
    CONSTRAINT ck_payment_reconciliations_status
        CHECK (status IN ('PENDING', 'RESOLVED', 'DEAD_LETTER'))
);

CREATE INDEX ix_payment_reconciliations_due ON payment_reconciliations (status, scheduled_at);

-- Un solo trabajo abierto por intento: la programación es idempotente.
CREATE UNIQUE INDEX uq_payment_reconciliations_attempt_open
    ON payment_reconciliations (payment_attempt_id) WHERE status = 'PENDING';
