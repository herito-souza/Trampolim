-- Reconcile app_users tables created before these fields were added to V1.
-- Defaults backfill existing accounts without changing their stored credentials.
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verification_token_hash VARCHAR(64);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMPTZ;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS login_locked_until TIMESTAMPTZ;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS diagnostic_data TEXT;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS favorite_opportunity_ids TEXT;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(64);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMPTZ;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS public_share_id VARCHAR(36);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS public_profile_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS share_location BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS share_summary BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS share_experiences BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS share_education BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS alert_preferences TEXT;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS pending_alert_ids TEXT;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS resume_file_name VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS resume_content_type VARCHAR(120);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS resume_content BYTEA;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS photo_file_name VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS photo_content_type VARCHAR(80);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS photo_content BYTEA;
