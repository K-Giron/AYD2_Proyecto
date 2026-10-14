-- Ciclo de vida de la obligación de crédito: vencida, pagada o cancelada.
ALTER TABLE credit_operations
    ADD COLUMN overdue_at TIMESTAMPTZ,
    ADD COLUMN settled_at TIMESTAMPTZ,
    ADD COLUMN cancelled_at TIMESTAMPTZ;

ALTER TABLE credit_operations DROP CONSTRAINT ck_credit_operation_status;
ALTER TABLE credit_operations ADD CONSTRAINT ck_credit_operation_status
    CHECK (status IN ('GRANTED', 'REJECTED', 'OVERDUE', 'SETTLED', 'CANCELLED'));

-- Solo una operación rechazada carece de vencimiento.
ALTER TABLE credit_operations DROP CONSTRAINT ck_credit_operation_due_date;
ALTER TABLE credit_operations ADD CONSTRAINT ck_credit_operation_due_date CHECK (
    (status = 'REJECTED' AND due_date IS NULL)
    OR (status <> 'REJECTED' AND due_date IS NOT NULL)
);

-- Cada marca temporal solo existe en el estado que la produce.
ALTER TABLE credit_operations ADD CONSTRAINT ck_credit_operation_transition_times CHECK (
    (settled_at IS NULL OR status = 'SETTLED')
    AND (cancelled_at IS NULL OR status = 'CANCELLED')
    AND (overdue_at IS NULL OR status IN ('OVERDUE', 'SETTLED'))
);

CREATE INDEX ix_credit_operations_status_due
    ON credit_operations (status, due_date);
