-- Extends verified awards; no duplicate credential or identity store.
ALTER TABLE learning_competency_awards ADD COLUMN issuer_name VARCHAR(180) NOT NULL DEFAULT 'Trampolim Capacita';
ALTER TABLE learning_competency_awards ADD COLUMN valid_until TIMESTAMPTZ;
ALTER TABLE learning_competency_awards ADD COLUMN revoked_at TIMESTAMPTZ;
ALTER TABLE learning_competency_awards ADD COLUMN revocation_reason VARCHAR(500);
ALTER TABLE learning_modules ADD COLUMN adaptive_skip_mastered BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE learning_modules ADD COLUMN adaptive_revision INTEGER NOT NULL DEFAULT 0;
ALTER TABLE learning_modules ADD COLUMN credential_validity_days INTEGER CHECK(credential_validity_days BETWEEN 1 AND 3650);
CREATE TABLE learning_module_prerequisites (
 module_id BIGINT NOT NULL REFERENCES learning_modules(id) ON DELETE CASCADE,
 required_module_id BIGINT NOT NULL REFERENCES learning_modules(id) ON DELETE CASCADE,
 PRIMARY KEY(module_id,required_module_id),CHECK(module_id<>required_module_id)
);
CREATE FUNCTION learning_award_validity() RETURNS trigger AS $$
DECLARE days INTEGER;
BEGIN
 SELECT credential_validity_days INTO days FROM learning_modules WHERE id=NEW.module_id;
 IF days IS NOT NULL THEN NEW.valid_until=NEW.awarded_at+make_interval(days=>days); END IF;
 RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_learning_award_validity BEFORE INSERT ON learning_competency_awards FOR EACH ROW EXECUTE FUNCTION learning_award_validity();
CREATE INDEX idx_award_active_skill ON learning_competency_awards(student_id,skill_code,skill_level) WHERE revoked_at IS NULL;
CREATE TABLE learning_adaptive_goals (
 id UUID PRIMARY KEY,student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 title VARCHAR(180) NOT NULL,source VARCHAR(12) NOT NULL CHECK(source IN ('MANUAL','COMPANY','FUTURE')),
 source_id BIGINT,requirements JSONB NOT NULL,source_title VARCHAR(180) NOT NULL DEFAULT '',source_checked_at TIMESTAMPTZ,
 revision INTEGER NOT NULL DEFAULT 1,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK((source='MANUAL' AND source_id IS NULL) OR (source<>'MANUAL' AND source_id>0)),
 cached_fingerprint VARCHAR(64),cached_result JSONB
);
CREATE INDEX idx_adaptive_goal_student ON learning_adaptive_goals(student_id,created_at DESC);
CREATE TABLE learning_passport_shares (
 id UUID PRIMARY KEY,student_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,recipient_email VARCHAR(180) NOT NULL,holder_label VARCHAR(80) NOT NULL DEFAULT '',
 consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,expires_at TIMESTAMPTZ NOT NULL,revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_passport_share_owner ON learning_passport_shares(student_id,consent_at DESC);
CREATE TABLE learning_passport_share_items (
 share_id UUID NOT NULL REFERENCES learning_passport_shares(id) ON DELETE CASCADE,
 award_id UUID NOT NULL REFERENCES learning_competency_awards(id) ON DELETE CASCADE,PRIMARY KEY(share_id,award_id)
);
CREATE TABLE learning_radar_invalidation_outbox (
 receipt_id UUID PRIMARY KEY REFERENCES learning_radar_exports(id) ON DELETE CASCADE,
 version BIGINT NOT NULL DEFAULT 1,delivered_version BIGINT NOT NULL DEFAULT 0,
 attempts INTEGER NOT NULL DEFAULT 0,next_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_radar_outbox_pending ON learning_radar_invalidation_outbox(next_at) WHERE version>delivered_version;
