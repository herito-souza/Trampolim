-- Additive evolution of the existing corporate applications; no applicant data is rewritten.
ALTER TABLE company_applications DROP CONSTRAINT company_applications_status_check;
ALTER TABLE company_applications ADD CONSTRAINT company_applications_status_check
 CHECK (status IN ('RECEIVED','IN_REVIEW','CONTACTED','INTERVIEW','ACCEPTED','DECLINED','CLOSED'));
ALTER TABLE company_applications ADD COLUMN revision BIGINT NOT NULL DEFAULT 0 CHECK (revision>=0);
CREATE INDEX idx_company_applications_stage_page ON company_applications(listing_id,status,created_at DESC,id DESC);
CREATE INDEX idx_company_listings_company_id ON company_listings(company_id,id);

CREATE TABLE company_recruitment_notes (
 id UUID PRIMARY KEY,
 application_id BIGINT NOT NULL REFERENCES company_applications(id) ON DELETE CASCADE,
 author_user_id BIGINT REFERENCES company_users(id) ON DELETE SET NULL,
 body VARCHAR(2000),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 expires_at TIMESTAMPTZ NOT NULL,
 deleted_at TIMESTAMPTZ,
 CHECK (body IS NOT NULL OR deleted_at IS NOT NULL)
);
CREATE INDEX idx_recruitment_notes_page ON company_recruitment_notes(application_id,created_at DESC,id DESC);
CREATE INDEX idx_recruitment_notes_expiry ON company_recruitment_notes(expires_at,id) WHERE body IS NOT NULL;
CREATE INDEX idx_recruitment_notes_tombstones ON company_recruitment_notes(created_at,id) WHERE body IS NULL;
CREATE TABLE company_recruitment_events (
 id BIGSERIAL PRIMARY KEY,
 application_id BIGINT NOT NULL REFERENCES company_applications(id) ON DELETE CASCADE,
 revision BIGINT NOT NULL CHECK (revision>=0),
 event_type VARCHAR(24) NOT NULL CHECK (event_type IN ('RECEIVED','BASELINE','STAGE_CHANGED','NOTE_ADDED','NOTE_REMOVED')),
 previous_status VARCHAR(24),
 status VARCHAR(24) NOT NULL,
 actor_user_id BIGINT REFERENCES company_users(id) ON DELETE SET NULL,
 request_key UUID,
 request_hash VARCHAR(64),
 note_id UUID,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(application_id,revision),
 UNIQUE(application_id,request_key)
);
CREATE INDEX idx_recruitment_events_page ON company_recruitment_events(application_id,id DESC);
CREATE INDEX idx_recruitment_events_retention ON company_recruitment_events(created_at,id) WHERE revision>0;
-- Snapshot at migration time: do not invent previous transitions or their authors/dates.
INSERT INTO company_recruitment_events(application_id,revision,event_type,status)
 SELECT id,revision,'BASELINE',status FROM company_applications;
CREATE FUNCTION record_company_application_received() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO company_recruitment_events(application_id,revision,event_type,status)
 VALUES (NEW.id,NEW.revision,'RECEIVED',NEW.status);
 RETURN NEW;
END $$;
CREATE TRIGGER company_application_received AFTER INSERT ON company_applications
 FOR EACH ROW EXECUTE FUNCTION record_company_application_received();
