-- Idempotent mapping from legacy Oportunidades company/listing IDs to Empresas.
ALTER TABLE company_users ALTER COLUMN terms_accepted_at DROP NOT NULL;
ALTER TABLE companies ADD COLUMN legacy_company_id BIGINT UNIQUE;
ALTER TABLE company_listings ADD COLUMN legacy_listing_id BIGINT UNIQUE;
ALTER TABLE company_listings ADD COLUMN legacy_publication_status VARCHAR(32);
ALTER TABLE company_listings ADD COLUMN legacy_payment_status VARCHAR(32);
ALTER TABLE company_listings ADD COLUMN legacy_price_cents INTEGER;
ALTER TABLE company_applications ADD COLUMN legacy_application_id BIGINT UNIQUE;
ALTER TABLE company_listing_events ADD COLUMN legacy_event_id BIGINT UNIQUE;

-- Historical charges are retained for reconciliation only. They never mark a
-- migrated listing as paid or expose the old checkout URL in the new product.
CREATE TABLE company_legacy_payment_history (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,
    legacy_payment_id BIGINT NOT NULL UNIQUE,
    provider VARCHAR(40),
    external_reference VARCHAR(120),
    provider_order_id VARCHAR(120),
    amount_cents INTEGER,
    legacy_status VARCHAR(40),
    provider_status VARCHAR(80),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_company_legacy_payment_history_listing ON company_legacy_payment_history(listing_id, created_at DESC);

-- Legacy team members must establish independent accounts and be invited again.
-- This table preserves the prior roster, but no old credentials or invite tokens.
CREATE TABLE company_legacy_members (
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    legacy_user_id BIGINT NOT NULL,
    email VARCHAR(180) NOT NULL,
    full_name VARCHAR(120),
    legacy_role VARCHAR(32) NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(company_id, legacy_user_id)
);

CREATE TABLE company_legacy_invites (
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    legacy_invite_id BIGINT NOT NULL UNIQUE,
    email VARCHAR(180) NOT NULL,
    legacy_role VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(company_id, legacy_invite_id)
);
