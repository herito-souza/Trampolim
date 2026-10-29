-- Organization and association data owned by the Empresas trail. Legacy IDs are
-- retained as provenance only; no cross-database foreign keys are created.
CREATE TABLE enterprise_organizations (
    id BIGSERIAL PRIMARY KEY,
    legacy_organization_id BIGINT UNIQUE,
    name VARCHAR(180) NOT NULL,
    organization_type VARCHAR(24) NOT NULL CHECK (organization_type IN ('ASSOCIATION', 'EDUCATIONAL', 'QUALIFICATION')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PENDING', 'SUSPENDED')),
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_enterprise_organizations_type_status ON enterprise_organizations(organization_type,status);

-- Memberships retain the former person's email and role, but never their old
-- Oportunidades user ID as an authentication identity or any credential.
CREATE TABLE enterprise_organization_members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
    legacy_user_id BIGINT,
    email VARCHAR(180) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('ASSOCIATION_ADMIN','ASSOCIATION_MEMBER','EDUCATION_ADMIN','CONTENT_EDITOR','PARTNER_MANAGER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_enterprise_org_member_email_role ON enterprise_organization_members(organization_id,LOWER(email),role);
CREATE INDEX idx_enterprise_org_members_email ON enterprise_organization_members(LOWER(email),organization_id);

CREATE TABLE enterprise_organization_invites (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('ASSOCIATION_ADMIN','ASSOCIATION_MEMBER','EDUCATION_ADMIN','CONTENT_EDITOR','PARTNER_MANAGER')),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_enterprise_org_invites_org ON enterprise_organization_invites(organization_id,created_at DESC);

CREATE TABLE enterprise_organization_companies (
    organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    legacy_child_organization_id BIGINT,
    relationship_type VARCHAR(32) NOT NULL CHECK (relationship_type IN ('ASSOCIATION_MEMBER','EDUCATION_PARTNER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','PENDING','ENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id,company_id,relationship_type)
);
CREATE INDEX idx_enterprise_org_companies_company ON enterprise_organization_companies(company_id,status);

CREATE TABLE enterprise_organization_audit_events (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES enterprise_organizations(id) ON DELETE SET NULL,
    actor_email VARCHAR(180),
    event_type VARCHAR(48) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_reference VARCHAR(120),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_enterprise_org_audit_org_created ON enterprise_organization_audit_events(organization_id,created_at DESC);
