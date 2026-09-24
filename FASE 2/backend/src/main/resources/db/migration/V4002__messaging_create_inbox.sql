CREATE TABLE inbox_messages (
    consumer_name VARCHAR(120) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    correlation_id UUID NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL,
    CONSTRAINT pk_inbox_messages PRIMARY KEY (consumer_name, event_id),
    CONSTRAINT ck_inbox_status CHECK (status IN ('RECEIVED', 'PROCESSED'))
);

CREATE INDEX ix_inbox_event ON inbox_messages (event_id);
CREATE INDEX ix_inbox_status_time ON inbox_messages (status, received_at);
