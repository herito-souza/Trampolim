CREATE TABLE premium_interests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    interest_type VARCHAR(24) NOT NULL CHECK (interest_type IN ('PILOT', 'GUIDANCE')),
    details VARCHAR(600) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_premium_interest_user_type UNIQUE (user_id, interest_type)
);

CREATE INDEX idx_premium_interests_updated ON premium_interests(updated_at DESC);
