CREATE TABLE learning_assessments (
 id BIGSERIAL PRIMARY KEY,
 course_id BIGINT NOT NULL REFERENCES learning_courses(id) ON DELETE CASCADE,
 subject_id BIGINT REFERENCES learning_subjects(id) ON DELETE SET NULL,
 module_id BIGINT REFERENCES learning_modules(id) ON DELETE SET NULL,
 title VARCHAR(180) NOT NULL,
 topic VARCHAR(180) NOT NULL DEFAULT '',
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','HIDDEN')),
 draft JSONB NOT NULL,
 current_version_id BIGINT,
 revision INTEGER NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(status<>'PUBLISHED' OR current_version_id IS NOT NULL)
);
CREATE TABLE learning_assessment_versions (
 id BIGSERIAL PRIMARY KEY,
 assessment_id BIGINT NOT NULL REFERENCES learning_assessments(id) ON DELETE CASCADE,
 version_number INTEGER NOT NULL,
 definition JSONB NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(assessment_id,version_number),
 UNIQUE(assessment_id,id)
);
ALTER TABLE learning_assessments ADD CONSTRAINT fk_assessment_current_version FOREIGN KEY(id,current_version_id) REFERENCES learning_assessment_versions(assessment_id,id) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX idx_assessment_course ON learning_assessments(course_id,status,id);
CREATE TABLE learning_assessment_attempts (
 id UUID PRIMARY KEY,
 assessment_id BIGINT NOT NULL REFERENCES learning_assessments(id) ON DELETE CASCADE,
 version_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 attempt_number INTEGER NOT NULL CHECK(attempt_number>0),
 status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS' CHECK(status IN ('IN_PROGRESS','SUBMITTED')),
 draft_answers JSONB NOT NULL DEFAULT '{}',
 answers JSONB,
 submission_key UUID,
 request_hash VARCHAR(64),
 earned_points INTEGER,
 total_points INTEGER,
 score_percent NUMERIC(6,2),
 passed BOOLEAN,
 criteria JSONB,
 started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 submitted_at TIMESTAMPTZ,
 FOREIGN KEY(assessment_id,version_id) REFERENCES learning_assessment_versions(assessment_id,id),
 UNIQUE(assessment_id,student_id,attempt_number),
 UNIQUE(id,student_id),
 CHECK(status<>'SUBMITTED' OR (answers IS NOT NULL AND submission_key IS NOT NULL AND earned_points IS NOT NULL AND total_points>0 AND score_percent BETWEEN 0 AND 100 AND passed IS NOT NULL AND criteria IS NOT NULL AND submitted_at IS NOT NULL))
);
CREATE UNIQUE INDEX idx_assessment_one_open_attempt ON learning_assessment_attempts(assessment_id,student_id) WHERE status='IN_PROGRESS';
CREATE INDEX idx_attempts_student ON learning_assessment_attempts(student_id,started_at DESC);
CREATE TABLE learning_competency_awards (
 id UUID PRIMARY KEY,
 student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 assessment_id BIGINT NOT NULL REFERENCES learning_assessments(id) ON DELETE CASCADE,
 attempt_id UUID NOT NULL,
 competency_key UUID NOT NULL,
 name VARCHAR(180) NOT NULL,
 description VARCHAR(2000) NOT NULL,
 course_id BIGINT NOT NULL REFERENCES learning_courses(id) ON DELETE CASCADE,
 module_id BIGINT NOT NULL REFERENCES learning_modules(id) ON DELETE CASCADE,
 course_title VARCHAR(180) NOT NULL,
 module_title VARCHAR(180) NOT NULL,
 teacher_name VARCHAR(180) NOT NULL,
 score_percent NUMERIC(6,2) NOT NULL,
 criterion VARCHAR(2000) NOT NULL,
 awarded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 highlighted BOOLEAN NOT NULL DEFAULT FALSE,
 sort_order INTEGER NOT NULL DEFAULT 0 CHECK(sort_order BETWEEN 0 AND 10000),
 share_hash VARCHAR(64) UNIQUE,
 share_expires_at TIMESTAMPTZ,
 share_consent_at TIMESTAMPTZ,
 FOREIGN KEY(attempt_id,student_id) REFERENCES learning_assessment_attempts(id,student_id) ON DELETE CASCADE,
 UNIQUE(student_id,assessment_id,competency_key)
);
CREATE INDEX idx_competency_student ON learning_competency_awards(student_id,highlighted DESC,sort_order,awarded_at DESC);
CREATE TABLE learning_assessment_audit (
 id BIGSERIAL PRIMARY KEY,
 actor_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 assessment_id BIGINT,
 resource_id UUID,
 event VARCHAR(40) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
