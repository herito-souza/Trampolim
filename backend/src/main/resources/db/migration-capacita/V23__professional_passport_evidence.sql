-- Professional evidence differs from an academic assessment award.
CREATE TABLE learning_professional_credentials (
 id UUID PRIMARY KEY,student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,state VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','ACTIVE','REVOKED')),
 evidence JSONB NOT NULL DEFAULT '{}',consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_professional_credential_owner ON learning_professional_credentials(student_id,consent_at DESC);
CREATE TABLE learning_professional_shares (
 id UUID PRIMARY KEY,credential_id UUID NOT NULL REFERENCES learning_professional_credentials(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,recipient_email VARCHAR(180) NOT NULL,expires_at TIMESTAMPTZ NOT NULL,revoked_at TIMESTAMPTZ,consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_professional_share_owner ON learning_professional_shares(credential_id,consent_at DESC);
