CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    email_verification_token_hash VARCHAR(64),
    email_verification_expires_at TIMESTAMPTZ,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    login_locked_until TIMESTAMPTZ,
    token_version INTEGER NOT NULL DEFAULT 0,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(40),
    location VARCHAR(120),
    summary VARCHAR(2000),
    diagnostic_data TEXT,
    favorite_opportunity_ids TEXT,
    password_reset_token_hash VARCHAR(64),
    password_reset_expires_at TIMESTAMPTZ,
    public_share_id VARCHAR(36) UNIQUE,
    public_profile_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    share_location BOOLEAN NOT NULL DEFAULT FALSE,
    share_summary BOOLEAN NOT NULL DEFAULT TRUE,
    share_experiences BOOLEAN NOT NULL DEFAULT TRUE,
    share_education BOOLEAN NOT NULL DEFAULT TRUE,
    alert_preferences TEXT,
    pending_alert_ids TEXT,
    resume_file_name VARCHAR(255),
    resume_content_type VARCHAR(120),
    resume_content BYTEA,
    photo_file_name VARCHAR(255),
    photo_content_type VARCHAR(80),
    photo_content BYTEA
);

CREATE TABLE catalog_items (
    id BIGSERIAL PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    title VARCHAR(180) NOT NULL,
    provider VARCHAR(180) NOT NULL,
    description VARCHAR(3000) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    area VARCHAR(32) NOT NULL,
    location VARCHAR(180),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    modality VARCHAR(40),
    experience_level VARCHAR(40),
    contract_type VARCHAR(40),
    work_schedule VARCHAR(80),
    salary VARCHAR(80),
    accessibility VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    reviewed_by VARCHAR(180),
    reviewed_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    expires_at DATE,
    last_validity_reminder_at TIMESTAMPTZ,
    last_link_check_at TIMESTAMPTZ,
    last_link_reminder_at TIMESTAMPTZ,
    link_status VARCHAR(20) DEFAULT 'UNKNOWN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE experiences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    position VARCHAR(140) NOT NULL,
    company VARCHAR(140) NOT NULL,
    start_date VARCHAR(7),
    end_date VARCHAR(7),
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(2000)
);

CREATE TABLE educations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    institution VARCHAR(140) NOT NULL,
    course VARCHAR(140) NOT NULL,
    level VARCHAR(80),
    start_date VARCHAR(7),
    end_date VARCHAR(7),
    in_progress BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'SAVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(2000),
    follow_up_date DATE,
    reminder_sent_at TIMESTAMPTZ,
    CONSTRAINT uq_job_application_user_item UNIQUE (user_id, item_id)
);

CREATE TABLE catalog_metrics (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(24) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    item_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_catalog_metrics_created_at ON catalog_metrics(created_at);
