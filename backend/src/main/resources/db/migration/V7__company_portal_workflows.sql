ALTER TABLE companies ADD COLUMN IF NOT EXISTS public_slug VARCHAR(36) UNIQUE;
UPDATE companies SET public_slug = gen_random_uuid()::text WHERE public_slug IS NULL;
ALTER TABLE companies ALTER COLUMN public_slug SET NOT NULL;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS profile_description VARCHAR(2000);
ALTER TABLE companies ALTER COLUMN public_slug SET DEFAULT gen_random_uuid()::text;

CREATE TABLE IF NOT EXISTS company_members (
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'RECRUITER', 'ANALYST')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (company_id, user_id)
);
INSERT INTO company_members(company_id, user_id, role)
SELECT id, user_id, 'OWNER' FROM companies ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS company_invites (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('RECRUITER', 'ANALYST')),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE company_listings ADD COLUMN IF NOT EXISTS expires_at DATE;
ALTER TABLE company_listings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE company_listings ALTER COLUMN payment_status SET DEFAULT 'NOT_PROCESSED';
ALTER TABLE company_listings ALTER COLUMN publication_status SET DEFAULT 'DRAFT';
UPDATE company_listings SET payment_status='NOT_PROCESSED', publication_status='DRAFT' WHERE publication_status='AWAITING_PAYMENT';

CREATE TABLE IF NOT EXISTS company_listing_events (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,
    event_type VARCHAR(24) NOT NULL CHECK (event_type IN ('VIEW', 'APPLICATION_CLICK')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_company_listing_events_listing ON company_listing_events(listing_id, created_at);

CREATE TABLE IF NOT EXISTS company_applications (
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
CREATE INDEX IF NOT EXISTS idx_company_applications_listing ON company_applications(listing_id, created_at DESC);
