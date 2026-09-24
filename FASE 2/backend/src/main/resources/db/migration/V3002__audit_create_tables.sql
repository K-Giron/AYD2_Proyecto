-- Módulo: audit (Edson Sosa) - AUD-01/AUD-02.
-- Evidencia auditable transversal conforme a CDU-12, ADR-002, ADR-006 y
-- ADR-007: actor, instante, cambio, resultado y correlación por orden o
-- transacción. Tabla de solo anexado: la aplicación no actualiza ni elimina.
-- Rango V3001-V3999 ratificado para integration/payments/audit.

CREATE TABLE audit_events (
    id              UUID PRIMARY KEY,
    event_id        UUID NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    order_id        UUID,
    actor           VARCHAR(100) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    result          VARCHAR(64) NOT NULL,
    before_state    TEXT,
    after_state     TEXT,
    correlation_id  UUID NOT NULL,
    causation_id    UUID,
    occurred_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_audit_events_event UNIQUE (event_id)
);

-- Índices para la consulta de evidencia de AUD-02 (< 1 s).
CREATE INDEX ix_audit_events_order ON audit_events (order_id, occurred_at DESC);
CREATE INDEX ix_audit_events_aggregate
    ON audit_events (aggregate_type, aggregate_id, occurred_at DESC);
CREATE INDEX ix_audit_events_correlation ON audit_events (correlation_id);
