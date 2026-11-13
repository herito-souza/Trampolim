CREATE TABLE IF NOT EXISTS domain_event_outbox (
    event_id UUID PRIMARY KEY,
    trail VARCHAR(24) NOT NULL CHECK (trail = 'OPORTUNIDADES'),
    event_name VARCHAR(120) NOT NULL,
    schema_version INTEGER NOT NULL CHECK (schema_version > 0),
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(240) NOT NULL UNIQUE,
    occurred_at TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(120),
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (state IN ('PENDING','PROCESSED','FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_domain_event_outbox_pending
    ON domain_event_outbox(next_attempt_at, occurred_at, event_id) WHERE state='PENDING';

CREATE TABLE IF NOT EXISTS domain_event_receipts (
    event_id UUID NOT NULL REFERENCES domain_event_outbox(event_id) ON DELETE CASCADE,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(event_id, consumer_name)
);
