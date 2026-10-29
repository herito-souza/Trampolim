ALTER TABLE companies ADD COLUMN public_slug VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text;
ALTER TABLE companies ADD COLUMN profile_description VARCHAR(2000);
ALTER TABLE company_listings ADD COLUMN expires_at DATE;
ALTER TABLE company_listings ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE company_applications (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,
    candidate_name VARCHAR(120) NOT NULL,
    candidate_email VARCHAR(180) NOT NULL,
    candidate_phone VARCHAR(40),
    message VARCHAR(2000),
    status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED' CHECK (status IN ('RECEIVED', 'IN_REVIEW', 'CONTACTED', 'ACCEPTED', 'DECLINED')),
    consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_company_application_email_listing UNIQUE (listing_id, candidate_email)
);
CREATE INDEX idx_company_applications_listing ON company_applications(listing_id, created_at DESC);

CREATE TABLE company_commercial_interests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    offer_type VARCHAR(32) NOT NULL CHECK (offer_type IN ('PLAN', 'HIGHLIGHT', 'RECRUITMENT', 'COURSE_PARTNERSHIP', 'INSTITUTIONAL', 'ANALYTICS')),
    title VARCHAR(160) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_company_commercial_interests_company ON company_commercial_interests(company_id, created_at DESC);

CREATE TABLE company_audit_events (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL,
    actor_user_id BIGINT,
    event_type VARCHAR(60) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
