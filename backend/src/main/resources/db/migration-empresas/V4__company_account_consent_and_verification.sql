ALTER TABLE company_users ADD COLUMN terms_accepted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE company_users ADD COLUMN email_verification_token_hash VARCHAR(64);
ALTER TABLE company_users ADD COLUMN email_verification_expires_at TIMESTAMPTZ;
