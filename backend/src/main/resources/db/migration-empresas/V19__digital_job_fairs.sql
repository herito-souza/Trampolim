-- Public event infrastructure owned by Empresas. No duplicated companies or vacancies.
ALTER TABLE hub_campaigns ADD CONSTRAINT uq_fair_campaign_organization UNIQUE(organization_id,id);
ALTER TABLE company_listings ADD CONSTRAINT uq_fair_listing_company UNIQUE(id,company_id);
CREATE TABLE job_fairs (
 id BIGSERIAL PRIMARY KEY,organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id),
 campaign_id BIGINT,title VARCHAR(180) NOT NULL,description VARCHAR(4000) NOT NULL,
 starts_at TIMESTAMPTZ NOT NULL,ends_at TIMESTAMPTZ NOT NULL CHECK(ends_at>starts_at),
 modality VARCHAR(16) NOT NULL CHECK(modality IN ('PRESENCIAL','DIGITAL','HIBRIDO')),
 municipality_code VARCHAR(7) CHECK(municipality_code IS NULL OR municipality_code~'^[0-9]{7}$'),state VARCHAR(2),venue VARCHAR(500) NOT NULL DEFAULT '',
 registration_enabled BOOLEAN NOT NULL DEFAULT FALSE,status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','CANCELLED','ARCHIVED')),
 revision BIGINT NOT NULL DEFAULT 1 CHECK(revision>0),create_key UUID NOT NULL,create_hash VARCHAR(64) NOT NULL,
 published_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(organization_id,campaign_id) REFERENCES hub_campaigns(organization_id,id),UNIQUE(organization_id,create_key),
 CHECK(modality='DIGITAL' OR (municipality_code IS NOT NULL AND length(trim(venue))>0))
);
CREATE INDEX idx_job_fair_org_page ON job_fairs(organization_id,id DESC);
CREATE INDEX idx_job_fair_public_location ON job_fairs(municipality_code,starts_at,id DESC) WHERE status IN ('PUBLISHED','CANCELLED');
CREATE INDEX idx_job_fair_public_period ON job_fairs(ends_at,id DESC) WHERE status IN ('PUBLISHED','CANCELLED');
CREATE TABLE job_fair_companies (
 event_id BIGINT NOT NULL REFERENCES job_fairs(id) ON DELETE CASCADE,company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
 status VARCHAR(16) NOT NULL DEFAULT 'INVITED' CHECK(status IN ('INVITED','ACCEPTED','WITHDRAWN')),revision BIGINT NOT NULL DEFAULT 0,
 accepted_at TIMESTAMPTZ,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(event_id,company_id)
);
CREATE INDEX idx_job_fair_company_invites ON job_fair_companies(company_id,event_id DESC);
CREATE TABLE job_fair_listings (
 event_id BIGINT NOT NULL,company_id BIGINT NOT NULL,listing_id BIGINT NOT NULL,
 PRIMARY KEY(event_id,listing_id),FOREIGN KEY(event_id,company_id) REFERENCES job_fair_companies(event_id,company_id) ON DELETE CASCADE,
 FOREIGN KEY(listing_id,company_id) REFERENCES company_listings(id,company_id) ON DELETE CASCADE
);
CREATE TABLE job_fair_courses(event_id BIGINT NOT NULL REFERENCES job_fairs(id) ON DELETE CASCADE,course_id BIGINT NOT NULL CHECK(course_id>0),PRIMARY KEY(event_id,course_id));
CREATE TABLE job_fair_sessions (
 id BIGSERIAL PRIMARY KEY,event_id BIGINT NOT NULL REFERENCES job_fairs(id) ON DELETE CASCADE,
 kind VARCHAR(16) NOT NULL CHECK(kind IN ('TALK','WORKSHOP')),title VARCHAR(180) NOT NULL,description VARCHAR(2000) NOT NULL,
 speaker_name VARCHAR(180) NOT NULL DEFAULT '',public_speaker_consent BOOLEAN NOT NULL DEFAULT FALSE CHECK(speaker_name='' OR public_speaker_consent),starts_at TIMESTAMPTZ NOT NULL,ends_at TIMESTAMPTZ NOT NULL CHECK(ends_at>starts_at)
);
CREATE INDEX idx_job_fair_agenda ON job_fair_sessions(event_id,starts_at,id);
CREATE TABLE job_fair_audit(id BIGSERIAL PRIMARY KEY,event_id BIGINT NOT NULL REFERENCES job_fairs(id) ON DELETE CASCADE,actor_user_id BIGINT REFERENCES company_users(id) ON DELETE SET NULL,action VARCHAR(40) NOT NULL,revision BIGINT NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
