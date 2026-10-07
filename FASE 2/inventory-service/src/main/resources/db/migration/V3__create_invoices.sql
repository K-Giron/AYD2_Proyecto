-- Módulo de finanzas: cuentas por pagar (proveedores) y por cobrar
-- (tiendas/clientes). Puede referenciar el folio de un movimiento, pero no
-- depende de él (relación informativa, sin FK).
CREATE TABLE invoices (
    id              UUID PRIMARY KEY,
    invoice_type    VARCHAR(16) NOT NULL,
    counterparty    VARCHAR(160) NOT NULL,
    description     VARCHAR(255),
    amount          NUMERIC(12,2) NOT NULL,
    related_folio   VARCHAR(16),
    due_date        DATE NOT NULL,
    status          VARCHAR(16) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    paid_at         TIMESTAMPTZ,
    CONSTRAINT ck_invoices_type CHECK (invoice_type IN ('PAYABLE', 'RECEIVABLE')),
    CONSTRAINT ck_invoices_status CHECK (status IN ('PENDING', 'PAID')),
    CONSTRAINT ck_invoices_amount_positive CHECK (amount > 0)
);

CREATE INDEX ix_invoices_type_status ON invoices (invoice_type, status);
