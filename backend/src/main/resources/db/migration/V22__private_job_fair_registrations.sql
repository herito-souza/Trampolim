-- Local registrations only; event/organizer references are validated via HTTP, not cross-database FKs.
CREATE TABLE job_fair_registrations (
 user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,event_id BIGINT NOT NULL CHECK(event_id>0),organization_id BIGINT NOT NULL CHECK(organization_id>0),
 receipt UUID NOT NULL UNIQUE,status VARCHAR(16) NOT NULL CHECK(status IN ('REGISTERED','WITHDRAWN')),revision BIGINT NOT NULL CHECK(revision>0),
 consent_at TIMESTAMPTZ NOT NULL,source_checked_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 request_key UUID,request_hash VARCHAR(64),PRIMARY KEY(user_id,event_id)
);
CREATE INDEX idx_job_fair_registration_counts ON job_fair_registrations(organization_id,event_id) WHERE status='REGISTERED';
CREATE INDEX idx_job_fair_registration_owner ON job_fair_registrations(user_id,event_id DESC);
