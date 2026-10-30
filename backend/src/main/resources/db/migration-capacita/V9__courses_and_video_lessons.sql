CREATE TABLE learning_account_roles (
    user_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK(role IN ('TEACHER','ADMIN')),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id,role)
);
CREATE TABLE learning_courses (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES student_users(id),
    title VARCHAR(180) NOT NULL,
    description VARCHAR(4000) NOT NULL DEFAULT '',
    category VARCHAR(120) NOT NULL,
    image_url VARCHAR(1000),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','HIDDEN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_learning_courses_teacher ON learning_courses(teacher_id,status);
CREATE TABLE learning_subjects (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES learning_courses(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK(sort_order>=0)
);
CREATE INDEX idx_learning_subjects_course ON learning_subjects(course_id,sort_order,id);
CREATE TABLE learning_modules (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL REFERENCES learning_subjects(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK(sort_order>=0)
);
CREATE INDEX idx_learning_modules_subject ON learning_modules(subject_id,sort_order,id);
CREATE TABLE learning_lessons (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL REFERENCES learning_modules(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(4000) NOT NULL DEFAULT '',
    objectives VARCHAR(4000) NOT NULL DEFAULT '',
    transcript TEXT,
    captions_vtt TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK(sort_order>=0),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','HIDDEN')),
    media_key UUID,
    media_type VARCHAR(40),
    media_bytes BIGINT,
    duration_seconds DOUBLE PRECISION,
    validation_status VARCHAR(16) NOT NULL DEFAULT 'MISSING' CHECK(validation_status IN ('MISSING','READY')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK(status<>'PUBLISHED' OR (validation_status='READY' AND media_key IS NOT NULL AND duration_seconds BETWEEN 180 AND 300))
);
CREATE INDEX idx_learning_lessons_module ON learning_lessons(module_id,sort_order,id);
CREATE TABLE learning_lesson_progress (
    student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES learning_lessons(id) ON DELETE CASCADE,
    position_seconds DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK(position_seconds>=0),
    watched_seconds DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK(watched_seconds>=0),
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(student_id,lesson_id)
);
CREATE TABLE learning_playback_sessions (
    id UUID PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES learning_lessons(id) ON DELETE CASCADE,
    last_position DOUBLE PRECISION NOT NULL DEFAULT 0,
    preview BOOLEAN NOT NULL DEFAULT FALSE,
    token_version INTEGER NOT NULL,
    last_event_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP+INTERVAL '30 minutes'
);
CREATE INDEX idx_learning_sessions_expiry ON learning_playback_sessions(expires_at);
CREATE TABLE learning_role_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    granted BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
