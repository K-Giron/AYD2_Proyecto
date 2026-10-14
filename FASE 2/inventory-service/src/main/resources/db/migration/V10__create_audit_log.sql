-- Bitácora transversal: trazo de folios, envíos y cuentas, con el actor
-- y el momento en que ocurrió cada evento.
CREATE TABLE audit_log_entries (
    id               UUID PRIMARY KEY,
    event_type       VARCHAR(48) NOT NULL,
    description      VARCHAR(500) NOT NULL,
    related_folio    VARCHAR(24),
    point_of_sale_id UUID,
    actor            VARCHAR(160) NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_audit_log_related_folio ON audit_log_entries (related_folio);
CREATE INDEX ix_audit_log_point_of_sale ON audit_log_entries (point_of_sale_id, occurred_at DESC);
CREATE INDEX ix_audit_log_occurred_at ON audit_log_entries (occurred_at DESC);
