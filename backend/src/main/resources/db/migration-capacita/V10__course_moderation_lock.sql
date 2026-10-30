ALTER TABLE learning_courses ADD COLUMN moderation_blocked BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE learning_upload_limits (
    user_id BIGINT PRIMARY KEY REFERENCES student_users(id) ON DELETE CASCADE,
    window_started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK(attempts>=0),
    active_until TIMESTAMPTZ
);
