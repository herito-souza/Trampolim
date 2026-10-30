CREATE TABLE learning_question_media (
    id UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    storage_key UUID NOT NULL UNIQUE,
    kind VARCHAR(8) NOT NULL CHECK(kind IN ('AUDIO','VIDEO')),
    media_type VARCHAR(40) NOT NULL,
    duration_seconds DOUBLE PRECISION NOT NULL CHECK(duration_seconds>0),
    size_bytes BIGINT NOT NULL CHECK(size_bytes>0),
    last_attached_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_question_media_owner ON learning_question_media(owner_id,created_at);
CREATE TABLE learning_questions (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES learning_courses(id) ON DELETE CASCADE,
    module_id BIGINT REFERENCES learning_modules(id) ON DELETE CASCADE,
    lesson_id BIGINT REFERENCES learning_lessons(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK(status IN ('WAITING','IN_PROGRESS','ANSWERED','CLOSED')),
    client_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    UNIQUE(student_id,client_key),
    CHECK(lesson_id IS NULL OR module_id IS NOT NULL)
);
CREATE INDEX idx_learning_questions_course_status ON learning_questions(course_id,status,updated_at DESC);
CREATE INDEX idx_learning_questions_student ON learning_questions(student_id,updated_at DESC);
CREATE TABLE learning_reply_batches (
    id UUID PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    client_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    question_ids BIGINT[] NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(author_id,client_key)
);
CREATE TABLE learning_question_messages (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES learning_questions(id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
    author_role VARCHAR(12) NOT NULL CHECK(author_role IN ('STUDENT','TEACHER')),
    body VARCHAR(6000) NOT NULL DEFAULT '',
    media_id UUID REFERENCES learning_question_media(id) ON DELETE SET NULL,
    transcript TEXT,
    captions_vtt TEXT,
    client_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    batch_id UUID REFERENCES learning_reply_batches(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(question_id,author_id,client_key)
);
CREATE INDEX idx_question_messages_thread ON learning_question_messages(question_id,id DESC);
CREATE INDEX idx_question_messages_media ON learning_question_messages(media_id);
CREATE TABLE learning_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES learning_questions(id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES learning_question_messages(id) ON DELETE CASCADE,
    kind VARCHAR(24) NOT NULL CHECK(kind IN ('QUESTION','FOLLOW_UP','REPLY','STATUS')),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id,message_id,kind)
);
CREATE INDEX idx_learning_notifications_cursor ON learning_notifications(user_id,id);
CREATE INDEX idx_learning_notifications_unread ON learning_notifications(user_id,id) WHERE read_at IS NULL;
CREATE TABLE learning_question_media_tickets (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL REFERENCES learning_question_media(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    token_version INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP+INTERVAL '10 minutes'
);
CREATE INDEX idx_question_media_tickets_expiry ON learning_question_media_tickets(expires_at);
CREATE TABLE learning_communication_uploads (
    user_id BIGINT PRIMARY KEY REFERENCES student_users(id) ON DELETE CASCADE,
    window_started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    active_until TIMESTAMPTZ
);
CREATE TABLE learning_question_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
    question_id BIGINT,
    event VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
