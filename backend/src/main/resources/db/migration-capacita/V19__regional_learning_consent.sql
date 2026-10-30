CREATE TABLE learning_observatory_consents (
 student_id BIGINT PRIMARY KEY REFERENCES student_users(id) ON DELETE CASCADE,region_code VARCHAR(2) NOT NULL,consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_learning_observatory_region ON learning_observatory_consents(region_code,student_id);
