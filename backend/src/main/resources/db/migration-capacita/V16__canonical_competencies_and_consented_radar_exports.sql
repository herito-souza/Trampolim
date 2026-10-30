ALTER TABLE learning_competency_awards ADD COLUMN skill_code VARCHAR(64);
ALTER TABLE learning_competency_awards ADD COLUMN skill_level INTEGER;
ALTER TABLE learning_competency_awards ADD CONSTRAINT ck_radar_award_mapping CHECK((skill_code IS NULL AND skill_level IS NULL) OR (skill_code IS NOT NULL AND skill_level IS NOT NULL AND skill_code ~ '^[a-z][a-z0-9-]{1,63}$' AND skill_level BETWEEN 1 AND 4));
ALTER TABLE learning_competency_awards ADD CONSTRAINT uq_award_radar_owner UNIQUE(id,student_id);
CREATE TABLE learning_radar_exports (
 id UUID PRIMARY KEY, award_id UUID NOT NULL REFERENCES learning_competency_awards(id) ON DELETE CASCADE,
 student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 challenge_hash VARCHAR(64) NOT NULL UNIQUE, status VARCHAR(16) NOT NULL CHECK(status IN ('PENDING','ACTIVE','REVOKED')),
 consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(award_id,student_id) REFERENCES learning_competency_awards(id,student_id) ON DELETE CASCADE
);
CREATE INDEX idx_learning_radar_export_owner ON learning_radar_exports(student_id,consent_at DESC);
CREATE INDEX idx_learning_award_radar_skill ON learning_competency_awards(skill_code) WHERE skill_code IS NOT NULL;
CREATE INDEX idx_assessment_canonical_skills ON learning_assessment_versions USING gin ((definition->'competencies') jsonb_path_ops);
