CREATE TABLE hub_profiles (
 organization_id BIGINT PRIMARY KEY REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
 legal_name VARCHAR(180) NOT NULL DEFAULT '',legal_identifier VARCHAR(80) NOT NULL DEFAULT '',
 contact_email VARCHAR(180) NOT NULL DEFAULT '',contact_phone VARCHAR(40) NOT NULL DEFAULT '',responsible_name VARCHAR(120) NOT NULL DEFAULT '',
 public_slug VARCHAR(80) UNIQUE,headline VARCHAR(180) NOT NULL DEFAULT '',about VARCHAR(5000) NOT NULL DEFAULT '',
 accent_color VARCHAR(7) NOT NULL DEFAULT '#9bc3e5',logo_key VARCHAR(100),published BOOLEAN NOT NULL DEFAULT FALSE,
 revision INTEGER NOT NULL DEFAULT 0,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE hub_institutional_plans (
 id BIGSERIAL PRIMARY KEY,code VARCHAR(60) NOT NULL UNIQUE,name VARCHAR(120) NOT NULL,
 billing_period VARCHAR(20) NOT NULL CHECK(billing_period IN ('MONTHLY','ANNUAL','LICENSE')),
 company_limit INTEGER NOT NULL CHECK(company_limit BETWEEN 1 AND 10000),program_limit INTEGER NOT NULL CHECK(program_limit BETWEEN 1 AND 200),
 member_limit INTEGER NOT NULL CHECK(member_limit BETWEEN 1 AND 2000),campaign_limit INTEGER NOT NULL CHECK(campaign_limit BETWEEN 1 AND 200),
 status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','ACTIVE','ARCHIVED')),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE hub_contracts (
 id BIGSERIAL PRIMARY KEY,organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
 plan_id BIGINT NOT NULL REFERENCES hub_institutional_plans(id),contract_reference VARCHAR(180) NOT NULL,
 starts_on DATE NOT NULL,ends_on DATE NOT NULL CHECK(ends_on>=starts_on),
 status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','ACTIVE','SUSPENDED','ENDED')),
 revision INTEGER NOT NULL DEFAULT 1,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_hub_live_contract ON hub_contracts(organization_id) WHERE status IN ('DRAFT','ACTIVE','SUSPENDED');
CREATE INDEX idx_hub_contract_org ON hub_contracts(organization_id,id DESC);
CREATE TABLE hub_programs (
 id BIGSERIAL PRIMARY KEY,organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
 title VARCHAR(180) NOT NULL,description VARCHAR(4000) NOT NULL,starts_on DATE NOT NULL,ends_on DATE NOT NULL CHECK(ends_on>=starts_on),
 status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED','ARCHIVED')),revision INTEGER NOT NULL DEFAULT 1,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_hub_program_org ON hub_programs(organization_id,id DESC);
CREATE INDEX idx_hub_public_program ON hub_programs(id DESC) WHERE status='PUBLISHED';
CREATE TABLE hub_program_courses (
 program_id BIGINT NOT NULL REFERENCES hub_programs(id) ON DELETE CASCADE,course_id BIGINT NOT NULL,
 title VARCHAR(180) NOT NULL,PRIMARY KEY(program_id,course_id)
);
CREATE TABLE hub_campaigns (
 id BIGSERIAL PRIMARY KEY,organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
 title VARCHAR(180) NOT NULL,body VARCHAR(3000) NOT NULL,starts_on DATE NOT NULL,ends_on DATE NOT NULL CHECK(ends_on>=starts_on),
 status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED','ARCHIVED')),revision INTEGER NOT NULL DEFAULT 1,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_hub_campaign_org ON hub_campaigns(organization_id,id DESC);
CREATE TABLE hub_company_invites (
 id BIGSERIAL PRIMARY KEY,organization_id BIGINT NOT NULL REFERENCES enterprise_organizations(id) ON DELETE CASCADE,
 email VARCHAR(180) NOT NULL,token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,accepted_company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL,accepted_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_hub_company_invites_org ON hub_company_invites(organization_id,id DESC);
CREATE INDEX idx_hub_association_active_companies ON enterprise_organization_companies(organization_id,company_id) WHERE status='ACTIVE' AND relationship_type='ASSOCIATION_MEMBER';
-- Applies limits even when an older association endpoint is used. Existing rows are preserved.
CREATE FUNCTION hub_enforce_capacity() RETURNS trigger AS $$
DECLARE oid BIGINT; capacity INTEGER; used BIGINT; plan RECORD;
BEGIN
 oid=NEW.organization_id;
 PERFORM id FROM enterprise_organizations WHERE id=oid FOR UPDATE;
 SELECT p.* INTO plan FROM hub_contracts c JOIN hub_institutional_plans p ON p.id=c.plan_id WHERE c.organization_id=oid AND c.status='ACTIVE' AND CURRENT_DATE BETWEEN c.starts_on AND c.ends_on ORDER BY c.id DESC LIMIT 1;
 IF TG_TABLE_NAME='enterprise_organization_companies' THEN
  IF TG_OP='INSERT' AND EXISTS(SELECT 1 FROM enterprise_organization_companies WHERE organization_id=oid AND company_id=NEW.company_id AND relationship_type=NEW.relationship_type) THEN RETURN NEW; END IF;
  IF NEW.relationship_type<>'ASSOCIATION_MEMBER' OR NEW.status='ENDED' THEN RETURN NEW; END IF;
  IF TG_OP='UPDATE' AND OLD.status<>'ENDED' THEN RETURN NEW; END IF;
  capacity=COALESCE(plan.company_limit,500);
  SELECT count(*) INTO used FROM enterprise_organization_companies WHERE organization_id=oid AND relationship_type='ASSOCIATION_MEMBER' AND status<>'ENDED';
 ELSIF TG_TABLE_NAME='enterprise_organization_members' THEN
  IF EXISTS(SELECT 1 FROM enterprise_organization_members WHERE organization_id=oid AND LOWER(email)=LOWER(NEW.email) ) THEN RETURN NEW; END IF;
  capacity=COALESCE(plan.member_limit,200);
  SELECT count(DISTINCT LOWER(email)) INTO used FROM enterprise_organization_members WHERE organization_id=oid;
 ELSIF TG_TABLE_NAME='hub_programs' THEN
  IF NEW.status='ARCHIVED' THEN RETURN NEW; END IF;
  IF TG_OP='UPDATE' AND OLD.status<>'ARCHIVED' THEN RETURN NEW; END IF;
  capacity=COALESCE(plan.program_limit,100);SELECT count(*) INTO used FROM hub_programs WHERE organization_id=oid AND status<>'ARCHIVED';
 ELSE
  IF NEW.status='ARCHIVED' THEN RETURN NEW; END IF;
  IF TG_OP='UPDATE' AND OLD.status<>'ARCHIVED' THEN RETURN NEW; END IF;
  capacity=COALESCE(plan.campaign_limit,30);SELECT count(*) INTO used FROM hub_campaigns WHERE organization_id=oid AND status<>'ARCHIVED';
 END IF;
 IF used>=capacity THEN RAISE EXCEPTION 'Institutional capacity reached' USING ERRCODE='23514',CONSTRAINT='hub_capacity'; END IF;
 RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_hub_company_capacity BEFORE INSERT OR UPDATE ON enterprise_organization_companies FOR EACH ROW EXECUTE FUNCTION hub_enforce_capacity();
CREATE TRIGGER trg_hub_member_capacity BEFORE INSERT ON enterprise_organization_members FOR EACH ROW EXECUTE FUNCTION hub_enforce_capacity();
CREATE TRIGGER trg_hub_program_capacity BEFORE INSERT OR UPDATE ON hub_programs FOR EACH ROW EXECUTE FUNCTION hub_enforce_capacity();
CREATE TRIGGER trg_hub_campaign_capacity BEFORE INSERT OR UPDATE ON hub_campaigns FOR EACH ROW EXECUTE FUNCTION hub_enforce_capacity();

INSERT INTO hub_profiles(organization_id) SELECT id FROM enterprise_organizations WHERE organization_type='ASSOCIATION' ON CONFLICT DO NOTHING;
CREATE FUNCTION hub_initialize_profile() RETURNS trigger AS $$ BEGIN IF NEW.organization_type='ASSOCIATION' THEN INSERT INTO hub_profiles(organization_id) VALUES (NEW.id) ON CONFLICT DO NOTHING; END IF; RETURN NEW; END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_hub_profile_initialize AFTER INSERT ON enterprise_organizations FOR EACH ROW EXECUTE FUNCTION hub_initialize_profile();
