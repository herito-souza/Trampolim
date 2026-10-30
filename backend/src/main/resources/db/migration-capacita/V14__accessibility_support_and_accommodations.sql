CREATE TABLE student_accessibility_preferences (
 user_id BIGINT PRIMARY KEY REFERENCES student_users(id) ON DELETE CASCADE,
 font_percent INTEGER NOT NULL DEFAULT 100 CHECK(font_percent IN (100,112,125,150)),
 high_contrast BOOLEAN NOT NULL DEFAULT FALSE,
 reduced_motion BOOLEAN NOT NULL DEFAULT FALSE,
 text_spacing BOOLEAN NOT NULL DEFAULT FALSE,
 playback_speed NUMERIC(3,2) NOT NULL DEFAULT 1 CHECK(playback_speed IN (0.75,1,1.25,1.5,2)),
 captions BOOLEAN NOT NULL DEFAULT TRUE,
 muted BOOLEAN NOT NULL DEFAULT FALSE,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE learning_support_requests (
 id BIGSERIAL PRIMARY KEY,
 student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 client_key UUID NOT NULL,
 message VARCHAR(4000) NOT NULL,
 reply VARCHAR(4000) NOT NULL DEFAULT '',
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','IN_PROGRESS','CLOSED')),
 consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(student_id,client_key)
);
CREATE INDEX idx_support_inbox ON learning_support_requests(status,id DESC);
CREATE TABLE learning_inclusive_teachers (
 user_id BIGINT PRIMARY KEY REFERENCES student_users(id) ON DELETE CASCADE,
 public_summary VARCHAR(1000) NOT NULL,
 verification_reference VARCHAR(1000) NOT NULL,
 verified BOOLEAN NOT NULL DEFAULT FALSE,
 verified_by BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 checked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE learning_assessment_accommodations (
 assessment_id BIGINT NOT NULL REFERENCES learning_assessments(id) ON DELETE CASCADE,
 student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 extended_due_at TIMESTAMPTZ NOT NULL,
 granted_by BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(assessment_id,student_id)
);
CREATE TABLE learning_inclusion_audit (
 id BIGSERIAL PRIMARY KEY,
 actor_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 target_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 event VARCHAR(40) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
