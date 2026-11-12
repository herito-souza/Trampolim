-- Additive local-only communication persistence. No credentials or external delivery.
CREATE TABLE communication_settings (
 user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
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
 id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
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
CREATE TABLE communication_preference_audit(id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,revision BIGINT NOT NULL,settings JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);

CREATE FUNCTION communication_application_event() RETURNS trigger AS $$ BEGIN
 IF NEW.status IS DISTINCT FROM OLD.status THEN
 INSERT INTO communication_events(source_key,kind,resource_id,scope_id) VALUES ('application:'||NEW.id||':'||gen_random_uuid(),'APPLICATION_UPDATED',NEW.id,NEW.user_id);END IF;RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_communication_application AFTER UPDATE OF status ON job_applications FOR EACH ROW EXECUTE FUNCTION communication_application_event();
CREATE FUNCTION communication_job_event() RETURNS trigger AS $$ BEGIN
 IF NEW.kind='job' AND NEW.active AND NEW.moderation_status='APPROVED' AND (NEW.expires_at IS NULL OR NEW.expires_at>=CURRENT_DATE) THEN
 IF TG_OP='INSERT' OR NOT (OLD.active AND OLD.moderation_status='APPROVED') THEN
 INSERT INTO communication_events(source_key,kind,resource_id,scope_id) VALUES ('catalog-job:'||NEW.id,'NEW_MATCH',NEW.id,NEW.id) ON CONFLICT DO NOTHING;END IF;END IF;RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_communication_job AFTER INSERT OR UPDATE OF active,moderation_status ON catalog_items FOR EACH ROW EXECUTE FUNCTION communication_job_event();

CREATE INDEX idx_communication_unread ON communication_deliveries(user_id) WHERE status='DELIVERED' AND read_at IS NULL;
CREATE INDEX idx_communication_kind_frequency ON communication_deliveries(user_id,kind,delivered_at DESC) WHERE status='DELIVERED';
