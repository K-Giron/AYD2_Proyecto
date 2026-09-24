-- CUN-11 / ADR-008: canales confirmados que Notifications puede resolver
-- sin consultar tablas de otro módulo.

CREATE TABLE customer_contacts (
    id                 UUID PRIMARY KEY,
    customer_id        UUID NOT NULL,
    channel            VARCHAR(16) NOT NULL,
    destination_token  VARCHAR(200) NOT NULL,
    enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    preference_rank    INTEGER NOT NULL,
    verified_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_customer_contacts_preference_rank CHECK (preference_rank > 0),
    CONSTRAINT uq_customer_contacts_destination UNIQUE (customer_id, channel, destination_token)
);

CREATE INDEX ix_customer_contacts_customer_preference
    ON customer_contacts (customer_id, enabled, preference_rank);

-- Contacto confirmado de la cuenta CLIENTE incluida en el recorrido E2E.
INSERT INTO customer_contacts (
    id, customer_id, channel, destination_token, enabled, preference_rank,
    verified_at, created_at, updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111401',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'EMAIL', 'cliente@megastore.test', TRUE, 1,
    TIMESTAMPTZ '2026-09-23 00:00:00+00',
    TIMESTAMPTZ '2026-09-23 00:00:00+00',
    TIMESTAMPTZ '2026-09-23 00:00:00+00'
);
