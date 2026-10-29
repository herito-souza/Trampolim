ALTER TABLE student_users ADD COLUMN password_reset_token_hash VARCHAR(64);
ALTER TABLE student_users ADD COLUMN password_reset_expires_at TIMESTAMPTZ;
