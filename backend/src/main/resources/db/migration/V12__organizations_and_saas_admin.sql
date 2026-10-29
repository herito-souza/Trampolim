ALTER TABLE app_users ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
ALTER TABLE app_users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS app_user_roles (
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role VARCHAR(24) NOT NULL CHECK (role IN ('CANDIDATE', 'STUDENT')),
    assignment_source VARCHAR(24) NOT NULL DEFAULT 'SELF_DECLARED' CHECK (assignment_source IN ('SYSTEM', 'SELF_DECLARED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role)
);
CREATE INDEX IF NOT EXISTS idx_app_user_roles_role ON app_user_roles(role, user_id);
INSERT INTO app_user_roles(user_id, role, assignment_source)
SELECT id, 'CANDIDATE', 'SYSTEM' FROM app_users WHERE account_type='PERSON'
ON CONFLICT (user_id, role) DO NOTHING;

CREATE OR REPLACE FUNCTION trampolim_assign_candidate_role() RETURNS trigger AS $$
BEGIN
    IF NEW.account_type='PERSON' THEN
        INSERT INTO app_user_roles(user_id,role,assignment_source) VALUES (NEW.id,'CANDIDATE','SYSTEM') ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_app_users_assign_candidate_role ON app_users;
CREATE TRIGGER trg_app_users_assign_candidate_role
AFTER INSERT ON app_users FOR EACH ROW EXECUTE FUNCTION trampolim_assign_candidate_role();

CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    organization_type VARCHAR(24) NOT NULL CHECK (organization_type IN ('COMPANY', 'ASSOCIATION', 'EDUCATIONAL', 'QUALIFICATION')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PENDING', 'SUSPENDED')),
    legacy_company_id BIGINT UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organization_members (
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL CHECK (role IN ('COMPANY_ADMIN', 'RECRUITER', 'ANALYST', 'ASSOCIATION_ADMIN', 'ASSOCIATION_MEMBER', 'EDUCATION_ADMIN', 'CONTENT_EDITOR', 'PARTNER_MANAGER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_organization_members_user ON organization_members(user_id, organization_id);

CREATE TABLE IF NOT EXISTS organization_relationships (
    parent_organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    child_organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    relationship_type VARCHAR(32) NOT NULL DEFAULT 'ASSOCIATION_MEMBER' CHECK (relationship_type IN ('ASSOCIATION_MEMBER', 'EDUCATION_PARTNER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PENDING', 'ENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (parent_organization_id, child_organization_id, relationship_type),
    CHECK (parent_organization_id <> child_organization_id)
);
CREATE INDEX IF NOT EXISTS idx_organization_relationships_child ON organization_relationships(child_organization_id, status);

CREATE TABLE IF NOT EXISTS platform_roles (
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL CHECK (role IN ('SUPER_ADMIN')),
    granted_by BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role)
);

-- Existing companies become organizations while retaining their original IDs and records.
INSERT INTO organizations(name, organization_type, status, legacy_company_id, created_at)
SELECT c.trade_name, 'COMPANY', 'ACTIVE', c.id, c.created_at
FROM companies c
ON CONFLICT (legacy_company_id) DO NOTHING;

INSERT INTO organization_members(organization_id, user_id, role)
SELECT o.id, cm.user_id,
       CASE cm.role WHEN 'OWNER' THEN 'COMPANY_ADMIN' WHEN 'RECRUITER' THEN 'RECRUITER' ELSE 'ANALYST' END
FROM company_members cm
JOIN organizations o ON o.legacy_company_id = cm.company_id
ON CONFLICT (organization_id, user_id) DO NOTHING;

-- Keep future company registrations and team changes represented in the shared org model.
CREATE OR REPLACE FUNCTION trampolim_sync_company_organization() RETURNS trigger AS $$
BEGIN
    INSERT INTO organizations(name, organization_type, status, legacy_company_id)
    VALUES (NEW.trade_name, 'COMPANY', 'ACTIVE', NEW.id)
    ON CONFLICT (legacy_company_id) DO UPDATE SET name = EXCLUDED.name, updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_companies_sync_organization ON companies;
CREATE TRIGGER trg_companies_sync_organization
AFTER INSERT OR UPDATE OF trade_name ON companies
FOR EACH ROW EXECUTE FUNCTION trampolim_sync_company_organization();

CREATE OR REPLACE FUNCTION trampolim_sync_company_member() RETURNS trigger AS $$
DECLARE org_id BIGINT;
BEGIN
    SELECT id INTO org_id FROM organizations WHERE legacy_company_id = COALESCE(NEW.company_id, OLD.company_id);
    IF TG_OP = 'DELETE' THEN
        DELETE FROM organization_members WHERE organization_id = org_id AND user_id = OLD.user_id;
        RETURN OLD;
    END IF;
    INSERT INTO organization_members(organization_id, user_id, role)
    VALUES (org_id, NEW.user_id,
            CASE NEW.role WHEN 'OWNER' THEN 'COMPANY_ADMIN' WHEN 'RECRUITER' THEN 'RECRUITER' ELSE 'ANALYST' END)
    ON CONFLICT (organization_id, user_id) DO UPDATE SET role = EXCLUDED.role;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_company_members_sync_organization ON company_members;
CREATE TRIGGER trg_company_members_sync_organization
AFTER INSERT OR UPDATE OR DELETE ON company_members
FOR EACH ROW EXECUTE FUNCTION trampolim_sync_company_member();

-- Subscription tables are intentionally empty until commercial terms are approved.
CREATE TABLE IF NOT EXISTS commercial_plans (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    billing_period VARCHAR(20) CHECK (billing_period IN ('MONTHLY', 'ANNUAL', 'ONE_TIME')),
    price_cents INTEGER CHECK (price_cents IS NULL OR price_cents >= 0),
    included_features JSONB NOT NULL DEFAULT '[]'::jsonb,
    usage_limits JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organization_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    plan_id BIGINT NOT NULL REFERENCES commercial_plans(id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING', 'TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED')),
    provider VARCHAR(32),
    provider_subscription_id VARCHAR(180),
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_org_subscriptions_org ON organization_subscriptions(organization_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_org_active_subscription ON organization_subscriptions(organization_id) WHERE status IN ('PENDING', 'TRIALING', 'ACTIVE', 'PAST_DUE');

CREATE TABLE IF NOT EXISTS organization_audit_events (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    actor_user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    event_type VARCHAR(48) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id BIGINT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_org_audit_events_org_created ON organization_audit_events(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_org_audit_events_actor_created ON organization_audit_events(actor_user_id, created_at DESC);
