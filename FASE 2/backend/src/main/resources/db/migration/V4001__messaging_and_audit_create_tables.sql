CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id UUID NOT NULL,
    idempotency_key VARCHAR(180) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_outbox_business_event
        UNIQUE (aggregate_id, event_type, idempotency_key),
    CONSTRAINT ck_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'DEAD_LETTER'))
);

CREATE INDEX ix_outbox_pending
    ON outbox_messages (status, available_at, created_at);
CREATE INDEX ix_outbox_aggregate
    ON outbox_messages (aggregate_type, aggregate_id);
CREATE INDEX ix_outbox_correlation
    ON outbox_messages (correlation_id);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(160) NOT NULL,
    actor_type VARCHAR(40) NOT NULL,
    action VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    order_id UUID,
    transaction_id UUID NOT NULL,
    correlation_id UUID NOT NULL,
    previous_state JSONB,
    new_state JSONB,
    result VARCHAR(24) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    evidence_hash VARCHAR(64) NOT NULL,
    CONSTRAINT ck_audit_result
        CHECK (result IN ('SUCCESS', 'REJECTED', 'FAILED'))
);

CREATE INDEX ix_audit_order_time ON audit_events (order_id, occurred_at);
CREATE INDEX ix_audit_transaction_time ON audit_events (transaction_id, occurred_at);
CREATE INDEX ix_audit_aggregate_time ON audit_events (aggregate_type, aggregate_id, occurred_at);
CREATE INDEX ix_audit_correlation ON audit_events (correlation_id);

CREATE TABLE audit_evidence_access (
    id UUID PRIMARY KEY,
    requester_subject_id VARCHAR(160) NOT NULL,
    requester_role VARCHAR(40) NOT NULL,
    action VARCHAR(24) NOT NULL,
    search_criteria_hash VARCHAR(64) NOT NULL,
    result VARCHAR(24) NOT NULL,
    evidence_reference VARCHAR(240),
    evidence_hash VARCHAR(64),
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_audit_access_action
        CHECK (action IN ('SEARCH', 'VIEW', 'EXPORT')),
    CONSTRAINT ck_audit_access_result
        CHECK (result IN ('SUCCESS', 'NOT_FOUND', 'REJECTED', 'FAILED'))
);

CREATE INDEX ix_audit_access_requester_time
    ON audit_evidence_access (requester_subject_id, requested_at);
