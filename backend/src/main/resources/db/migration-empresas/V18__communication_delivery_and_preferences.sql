-- Additive local-only communication persistence. No credentials or external delivery.
CREATE TABLE communication_settings (
 user_id BIGINT PRIMARY KEY REFERENCES company_users(id) ON DELETE CASCADE,
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
 id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES company_users(id) ON DELETE CASCADE,
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
CREATE TABLE communication_preference_audit(id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES company_users(id) ON DELETE CASCADE,revision BIGINT NOT NULL,settings JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);

CREATE FUNCTION communication_recruitment_event() RETURNS trigger AS $$ DECLARE company BIGINT; BEGIN
 IF NEW.event_type='STAGE_CHANGED' AND NEW.previous_status IS DISTINCT FROM NEW.status THEN
 SELECT l.company_id INTO company FROM company_applications a JOIN company_listings l ON l.id=a.listing_id WHERE a.id=NEW.application_id;
 INSERT INTO communication_events(source_key,kind,resource_id,scope_id) VALUES ('recruitment:'||NEW.id,'APPLICATION_UPDATED',NEW.application_id,company) ON CONFLICT DO NOTHING;END IF;RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_communication_recruitment AFTER INSERT ON company_recruitment_events FOR EACH ROW EXECUTE FUNCTION communication_recruitment_event();
CREATE FUNCTION communication_interview_event() RETURNS trigger AS $$ DECLARE company BIGINT; BEGIN
 IF NEW.interview_at IS NOT NULL THEN
 IF TG_OP='INSERT' OR NEW.interview_at IS DISTINCT FROM OLD.interview_at THEN
 SELECT l.company_id INTO company FROM company_applications a JOIN company_listings l ON l.id=a.listing_id WHERE a.id=NEW.application_id;
 INSERT INTO communication_events(source_key,kind,resource_id,scope_id) VALUES ('interview:'||NEW.id||':'||gen_random_uuid(),'INTERVIEW_INVITATION',NEW.application_id,company);END IF;END IF;RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_communication_interview AFTER INSERT OR UPDATE OF interview_at ON company_selection_outcomes FOR EACH ROW EXECUTE FUNCTION communication_interview_event();

CREATE INDEX idx_communication_unread ON communication_deliveries(user_id) WHERE status='DELIVERED' AND read_at IS NULL;
CREATE INDEX idx_communication_kind_frequency ON communication_deliveries(user_id,kind,delivered_at DESC) WHERE status='DELIVERED';
