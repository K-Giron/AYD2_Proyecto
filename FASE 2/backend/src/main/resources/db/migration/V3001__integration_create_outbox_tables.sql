-- Módulo: integration (Edson Sosa) - BB-04.
-- Outbox transaccional e Inbox de consumidores, conforme a ADR-002 y al
-- sobre de eventos docs/contracts/events/event-envelope-v1.md.
-- Rango V3001-V3999 ratificado para integration/payments/audit.

CREATE TABLE outbox_event (
    event_id          UUID PRIMARY KEY,
    aggregate_type    VARCHAR(64) NOT NULL,
    aggregate_id      UUID NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    event_version     INTEGER NOT NULL,
    payload           TEXT,
    correlation_id    UUID NOT NULL,
    causation_id      UUID,
    status            VARCHAR(16) NOT NULL,
    attempts          INTEGER NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ NOT NULL,
    failure_message   VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    published_at      TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_outbox_event_version_positive CHECK (event_version > 0),
    CONSTRAINT ck_outbox_event_attempts_non_negative CHECK (attempts >= 0),
    CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

-- Soporta el barrido del dispatcher: pendientes vencidos por fecha de creacion.
CREATE INDEX ix_outbox_event_pending
    ON outbox_event (status, next_attempt_at, created_at);

-- Libro idempotente del consumidor: un evento procesado una sola vez por cola.
CREATE TABLE inbox_event (
    id            UUID PRIMARY KEY,
    consumer      VARCHAR(100) NOT NULL,
    event_id      UUID NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_inbox_event_consumer_event UNIQUE (consumer, event_id)
);

CREATE INDEX ix_inbox_event_event_id ON inbox_event (event_id);
