CREATE TABLE feature_flags (
    flag_key VARCHAR(80) PRIMARY KEY CHECK (flag_key ~ '^[a-z][a-z0-9.-]{1,79}$'),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
    updated_by VARCHAR(180) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    targeting JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(targeting)='object')
);
CREATE TABLE feature_flag_audit (
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(80) NOT NULL,
    action VARCHAR(16) NOT NULL CHECK (action IN ('CREATED','UPDATED')),
    reason VARCHAR(500) NOT NULL,
    actor_email VARCHAR(180) NOT NULL,
    previous_state JSONB NOT NULL,
    current_state JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_feature_flag_audit_key_time ON feature_flag_audit(flag_key,created_at DESC);
