-- Additive local-only communication persistence. No credentials or external delivery.
CREATE TABLE communication_settings (
 user_id BIGINT PRIMARY KEY REFERENCES student_users(id) ON DELETE CASCADE,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,daily_limit INTEGER NOT NULL DEFAULT 100 CHECK(daily_limit BETWEEN 1 AND 500),
 minimum_interval_minutes INTEGER NOT NULL DEFAULT 0 CHECK(minimum_interval_minutes BETWEEN 0 AND 10080),
 categories JSONB NOT NULL DEFAULT '{}'::jsonb,revision BIGINT NOT NULL DEFAULT 0 CHECK(revision>=0),updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE communication_events (
 id BIGSERIAL PRIMARY KEY,source_key VARCHAR(160) NOT NULL UNIQUE,kind VARCHAR(40) NOT NULL,
 resource_id BIGINT NOT NULL CHECK(resource_id>0),scope_id BIGINT NOT NULL CHECK(scope_id>0),
 recipient_cursor BIGINT NOT NULL DEFAULT 0,reason VARCHAR(40),state VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','COMPLETED','FAILED')),
 attempts INTEGER NOT NULL DEFAULT 0,next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_communication_event_work ON communication_events(next_attempt_at,id) WHERE state='PENDING';
CREATE TABLE communication_deliveries (
 id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
 event_key VARCHAR(160) NOT NULL,kind VARCHAR(40) NOT NULL,resource_id BIGINT NOT NULL CHECK(resource_id>0),scope_id BIGINT NOT NULL CHECK(scope_id>0),
 legacy_kind VARCHAR(24),legacy_message_id BIGINT,legacy_id BIGINT,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','DELIVERED','SUPPRESSED','FAILED')),
 reason VARCHAR(40),attempts INTEGER NOT NULL DEFAULT 0,next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 title VARCHAR(180),body VARCHAR(1000),target VARCHAR(180),template_version INTEGER,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,delivered_at TIMESTAMPTZ,read_at TIMESTAMPTZ,
 UNIQUE(user_id,event_key)
);
CREATE INDEX idx_communication_delivery_work ON communication_deliveries(next_attempt_at,id) WHERE status='PENDING';
CREATE INDEX idx_communication_delivery_owner ON communication_deliveries(user_id,id DESC);
CREATE INDEX idx_communication_delivery_frequency ON communication_deliveries(user_id,delivered_at DESC) WHERE status='DELIVERED';
CREATE TABLE communication_preference_audit(id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,revision BIGINT NOT NULL,settings JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);

ALTER TABLE communication_deliveries ADD CONSTRAINT fk_communication_legacy_notice FOREIGN KEY(legacy_id) REFERENCES learning_notifications(id) ON DELETE CASCADE;
CREATE FUNCTION communication_course_event() RETURNS trigger AS $$ BEGIN
 IF (OLD.status='PUBLISHED' OR NEW.status='PUBLISHED') AND (NEW.title,NEW.description,NEW.status,NEW.category,NEW.moderation_blocked,NEW.teacher_id) IS DISTINCT FROM (OLD.title,OLD.description,OLD.status,OLD.category,OLD.moderation_blocked,OLD.teacher_id) THEN
 INSERT INTO communication_events(source_key,kind,resource_id,scope_id) VALUES ('course:'||NEW.id||':'||gen_random_uuid(),'COURSE_CHANGED',NEW.id,NEW.id);END IF;RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_communication_course AFTER UPDATE ON learning_courses FOR EACH ROW EXECUTE FUNCTION communication_course_event();
CREATE FUNCTION communication_assessment_event() RETURNS trigger AS $$ DECLARE course BIGINT; BEGIN
 SELECT course_id INTO course FROM learning_assessments WHERE id=NEW.assessment_id;
 INSERT INTO communication_events(source_key,kind,resource_id,scope_id) VALUES ('assessment-version:'||NEW.id,'ASSESSMENT_AVAILABLE',NEW.assessment_id,course) ON CONFLICT DO NOTHING;RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_communication_assessment AFTER INSERT ON learning_assessment_versions FOR EACH ROW EXECUTE FUNCTION communication_assessment_event();

CREATE INDEX idx_communication_unread ON communication_deliveries(user_id) WHERE status='DELIVERED' AND read_at IS NULL;
CREATE INDEX idx_communication_kind_frequency ON communication_deliveries(user_id,kind,delivered_at DESC) WHERE status='DELIVERED';
