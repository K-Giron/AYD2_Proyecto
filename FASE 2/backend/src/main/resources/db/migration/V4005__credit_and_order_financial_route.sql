-- Proyección local, decisiones trazables y obligaciones de crédito propio.
CREATE TABLE credit_customer_profiles (
    customer_id UUID PRIMARY KEY,
    completed_purchases INTEGER NOT NULL,
    on_time_payment_rate NUMERIC(5,4) NOT NULL,
    return_rate NUMERIC(5,4) NOT NULL,
    critical_incidents INTEGER NOT NULL,
    has_overdue_credit BOOLEAN NOT NULL,
    available_credit NUMERIC(14,2) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_credit_profile_purchases CHECK (completed_purchases >= 0),
    CONSTRAINT ck_credit_profile_on_time CHECK (on_time_payment_rate BETWEEN 0 AND 1),
    CONSTRAINT ck_credit_profile_returns CHECK (return_rate BETWEEN 0 AND 1),
    CONSTRAINT ck_credit_profile_incidents CHECK (critical_incidents >= 0),
    CONSTRAINT ck_credit_profile_available CHECK (available_credit >= 0)
);

CREATE TABLE credit_decisions (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    requested_amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    eligible BOOLEAN NOT NULL,
    approved_limit NUMERIC(14,2) NOT NULL,
    rule_version VARCHAR(64) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    correlation_id UUID NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_credit_decision_amount CHECK (requested_amount > 0),
    CONSTRAINT ck_credit_decision_limit CHECK (approved_limit >= 0)
);
CREATE INDEX ix_credit_decisions_customer_evaluated
    ON credit_decisions (customer_id, evaluated_at DESC);

CREATE TABLE credit_operations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    decision_id UUID NOT NULL REFERENCES credit_decisions (id),
    amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    approved_limit NUMERIC(14,2) NOT NULL,
    due_date DATE,
    rule_version VARCHAR(64) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    correlation_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_credit_operations_order UNIQUE (order_id),
    CONSTRAINT uq_credit_operations_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_credit_operation_amount CHECK (amount > 0),
    CONSTRAINT ck_credit_operation_status CHECK (status IN ('GRANTED', 'REJECTED')),
    CONSTRAINT ck_credit_operation_due_date CHECK (
        (status = 'GRANTED' AND due_date IS NOT NULL)
        OR (status = 'REJECTED' AND due_date IS NULL)
    )
);
CREATE INDEX ix_credit_operations_customer_created
    ON credit_operations (customer_id, created_at DESC);

ALTER TABLE customer_orders
    ADD COLUMN payment_route VARCHAR(24),
    ADD COLUMN financial_operation_id UUID,
    ADD CONSTRAINT ck_customer_orders_payment_route
        CHECK (payment_route IN ('PAYKLIK', 'OWN_CREDIT'));

ALTER TABLE customer_orders DROP CONSTRAINT ck_customer_orders_status;
ALTER TABLE customer_orders ADD CONSTRAINT ck_customer_orders_status CHECK (status IN (
    'CREATED','INVENTORY_RESERVED','PAYMENT_PENDING','PAYMENT_APPROVED','PAYMENT_REJECTED',
    'PAYMENT_INDETERMINATE','CREDIT_PENDING','CREDIT_APPROVED','CREDIT_REJECTED',
    'FISCAL_PENDING','FISCAL_APPROVED','FISCAL_REJECTED','CONFIRMED',
    'PREPARING','IN_TRANSIT','DELIVERED','CANCELLED'));

-- Perfil demostrativo proveniente de la proyección ERP local. Los demás
-- clientes empiezan sin historial y, por seguridad, no reciben crédito.
INSERT INTO credit_customer_profiles (
    customer_id, completed_purchases, on_time_payment_rate, return_rate,
    critical_incidents, has_overdue_credit, available_credit, observed_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 8, 1.0000, 0.1250,
    0, FALSE, 2500.00, CURRENT_TIMESTAMP
);
