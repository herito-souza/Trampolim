ALTER TABLE student_users ADD COLUMN email_verification_token_hash VARCHAR(64);
ALTER TABLE student_users ADD COLUMN email_verification_expires_at TIMESTAMPTZ;
