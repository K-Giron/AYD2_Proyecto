-- Historial del cliente recibido desde el ERP mediante el contrato credit-history-v1.
-- Es la fuente del perfil crediticio: el perfil se recalcula a partir de estos
-- registros, de modo que reprocesar el mismo dato no altera los totales.
CREATE TABLE credit_history_records (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    source_reference VARCHAR(160) NOT NULL,
    record_type VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    on_time BOOLEAN,
    critical BOOLEAN NOT NULL DEFAULT FALSE,
    contract_version VARCHAR(32) NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_credit_history_source UNIQUE (source_reference),
    CONSTRAINT ck_credit_history_type CHECK (record_type IN ('PURCHASE', 'PAYMENT', 'RETURN', 'INCIDENT')),
    CONSTRAINT ck_credit_history_amount CHECK (amount >= 0),
    CONSTRAINT ck_credit_history_on_time CHECK (record_type <> 'PAYMENT' OR on_time IS NOT NULL)
);
CREATE INDEX ix_credit_history_customer_occurred
    ON credit_history_records (customer_id, occurred_at DESC);
