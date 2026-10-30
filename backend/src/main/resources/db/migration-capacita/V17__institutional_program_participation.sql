CREATE TABLE hub_program_participations (
 student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 organization_id BIGINT NOT NULL CHECK(organization_id>0),program_id BIGINT NOT NULL CHECK(program_id>0),
 institution_name VARCHAR(180) NOT NULL,program_title VARCHAR(180) NOT NULL,
 consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(student_id,program_id)
);
CREATE INDEX idx_hub_student_cohort ON hub_program_participations(organization_id,program_id,student_id);
CREATE TABLE hub_participation_courses (
 student_id BIGINT NOT NULL,program_id BIGINT NOT NULL,course_id BIGINT NOT NULL REFERENCES learning_courses(id) ON DELETE CASCADE,
 PRIMARY KEY(student_id,program_id,course_id),
 FOREIGN KEY(student_id,program_id) REFERENCES hub_program_participations(student_id,program_id) ON DELETE CASCADE
);
CREATE INDEX idx_hub_participation_course ON hub_participation_courses(program_id,course_id,student_id);
