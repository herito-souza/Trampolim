-- Explicit, revocable account-to-evidence links; no mirrored student identity.
CREATE TABLE career_passport_links (
 token_hash VARCHAR(64) PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 receipt_id UUID NOT NULL REFERENCES career_challenge_submissions(receipt_id) ON DELETE CASCADE,
 credential_id UUID UNIQUE,expires_at TIMESTAMPTZ NOT NULL,consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_career_passport_owner ON career_passport_links(user_id,consent_at DESC);
CREATE UNIQUE INDEX uq_career_passport_active_receipt ON career_passport_links(receipt_id) WHERE credential_id IS NOT NULL AND revoked_at IS NULL;
ALTER TABLE career_challenge_submissions ADD COLUMN accepted_revision INTEGER;
