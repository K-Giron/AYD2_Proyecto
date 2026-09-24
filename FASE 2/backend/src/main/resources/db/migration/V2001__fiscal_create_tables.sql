-- Modulo: fiscal (Andy Sanchez) - FIS-01/FIS-02.
-- No referencia tablas privadas de Orders ni Payments; conserva sus UUID.

CREATE TABLE fiscal_documents (
    id                    UUID PRIMARY KEY,
    order_id              UUID NOT NULL,
    payment_attempt_id    UUID NOT NULL,
    document_type         VARCHAR(24) NOT NULL,
    original_document_id  UUID,
    tax_identifier        VARCHAR(32) NOT NULL,
    customer_name         VARCHAR(200) NOT NULL,
    total                 NUMERIC(14,2) NOT NULL,
    currency              VARCHAR(3) NOT NULL,
    cui                   VARCHAR(64) NOT NULL,
    series                VARCHAR(32) NOT NULL,
    xml_payload           TEXT NOT NULL,
    status                VARCHAR(24) NOT NULL,
    sat_authorization     VARCHAR(100),
    failure_code          VARCHAR(64),
    failure_message       VARCHAR(500),
    correlation_id        UUID NOT NULL,
    idempotency_key       VARCHAR(128) NOT NULL,
    submission_attempts   INTEGER NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    confirmed_at          TIMESTAMPTZ,
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_fiscal_documents_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_fiscal_documents_cui UNIQUE (cui),
    CONSTRAINT fk_fiscal_documents_original FOREIGN KEY (original_document_id)
        REFERENCES fiscal_documents (id),
    CONSTRAINT ck_fiscal_documents_total_positive CHECK (total > 0),
    CONSTRAINT ck_fiscal_documents_credit_original CHECK (
        document_type <> 'CREDIT_NOTE' OR original_document_id IS NOT NULL
    )
);

CREATE INDEX ix_fiscal_documents_order_created
    ON fiscal_documents (order_id, created_at DESC);
CREATE INDEX ix_fiscal_documents_status ON fiscal_documents (status);
