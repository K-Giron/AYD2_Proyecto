-- Una factura puede originarse en PayKlik o en crédito propio. Se conserva la
-- columna histórica durante la transición para no invalidar instalaciones F2.
ALTER TABLE fiscal_documents
    ADD COLUMN financial_operation_id UUID,
    ADD COLUMN financial_operation_type VARCHAR(24);

UPDATE fiscal_documents
SET financial_operation_id = payment_attempt_id,
    financial_operation_type = 'PAYKLIK'
WHERE financial_operation_id IS NULL;

ALTER TABLE fiscal_documents
    ALTER COLUMN financial_operation_id SET NOT NULL,
    ALTER COLUMN financial_operation_type SET NOT NULL,
    ALTER COLUMN payment_attempt_id DROP NOT NULL,
    ADD CONSTRAINT ck_fiscal_financial_operation_type
        CHECK (financial_operation_type IN ('PAYKLIK', 'OWN_CREDIT'));

CREATE INDEX ix_fiscal_documents_financial_operation
    ON fiscal_documents (financial_operation_type, financial_operation_id);
