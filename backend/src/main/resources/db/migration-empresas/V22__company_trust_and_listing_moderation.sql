-- Legacy registrations and approval statuses are preserved; no automatic verified badge.
CREATE TABLE company_trust_cases(id BIGSERIAL PRIMARY KEY,company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,state VARCHAR(16) NOT NULL DEFAULT 'REQUESTED' CHECK(state IN ('REQUESTED','UNDER_REVIEW','APPROVED','REJECTED')),profile_hash VARCHAR(64) NOT NULL,revision BIGINT NOT NULL DEFAULT 1,source_reference VARCHAR(500),registry_checked_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,decision_reason VARCHAR(1000),reviewed_by VARCHAR(180),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE UNIQUE INDEX uq_company_trust_open ON company_trust_cases(company_id) WHERE state IN ('REQUESTED','UNDER_REVIEW');
CREATE INDEX idx_company_trust_queue ON company_trust_cases(state,id);
CREATE TABLE company_trust_audit(id BIGSERIAL PRIMARY KEY,company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,listing_id BIGINT REFERENCES company_listings(id) ON DELETE SET NULL,actor VARCHAR(180) NOT NULL,action VARCHAR(40) NOT NULL,reason VARCHAR(1000) NOT NULL DEFAULT '',created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
ALTER TABLE company_listings ADD COLUMN trust_revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE company_listings ADD COLUMN risk_state VARCHAR(12) NOT NULL DEFAULT 'CLEAR' CHECK(risk_state IN ('CLEAR','REVIEW','HELD'));
ALTER TABLE company_listings ADD COLUMN risk_fingerprint VARCHAR(32);
ALTER TABLE company_listings ADD COLUMN risk_approved_fingerprint VARCHAR(32);
ALTER TABLE company_listings ADD COLUMN risk_signals JSONB NOT NULL DEFAULT '[]';
CREATE FUNCTION trust_listing_fingerprint(title TEXT,description TEXT,requirements TEXT,area TEXT,location TEXT,url TEXT) RETURNS TEXT LANGUAGE SQL IMMUTABLE AS $$ SELECT md5(geo_normalize(title)||'|'||geo_normalize(description)||'|'||geo_normalize(requirements)||'|'||geo_normalize(area)||'|'||geo_normalize(location)||'|'||lower(COALESCE(url,''))) $$;
UPDATE company_listings SET risk_fingerprint=trust_listing_fingerprint(title,description||'|'||COALESCE(benefits,'')||'|'||COALESCE(prerequisites,'')||'|'||COALESCE(salary,'')||'|'||contact_email||'|'||COALESCE(accepted_courses,''),requirements,area,location,url);
CREATE INDEX idx_trust_listing_fingerprint ON company_listings(risk_fingerprint,company_id,created_at DESC) WHERE kind='job';
CREATE INDEX idx_trust_listing_review ON company_listings(risk_state,id) WHERE risk_state<>'CLEAR';
CREATE INDEX idx_trust_company_created ON company_listings(company_id,created_at DESC);
CREATE TABLE company_listing_publish_events(id BIGSERIAL PRIMARY KEY,company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX idx_trust_publish_velocity ON company_listing_publish_events(company_id,created_at DESC);
CREATE TABLE company_listing_reports(receipt UUID PRIMARY KEY,listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,origin VARCHAR(16) NOT NULL CHECK(origin IN ('OPORTUNIDADES','CAPACITA')),reason VARCHAR(24) NOT NULL,details VARCHAR(1000) NOT NULL DEFAULT '',payload_hash VARCHAR(64) NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','RESOLVED','DISMISSED','WITHDRAWN')),revision BIGINT NOT NULL DEFAULT 1,decision_reason VARCHAR(1000),reviewed_by VARCHAR(180),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX idx_trust_report_queue ON company_listing_reports(status,created_at,receipt);
CREATE TABLE company_report_tombstones(receipt UUID PRIMARY KEY,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE company_listing_appeals(id BIGSERIAL PRIMARY KEY,company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,reason VARCHAR(1000) NOT NULL,state VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','RESOLVED')),created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE UNIQUE INDEX uq_trust_open_appeal ON company_listing_appeals(listing_id) WHERE state='OPEN';
CREATE FUNCTION trust_listing_guard() RETURNS trigger AS $$ DECLARE body TEXT;signals JSONB:='[]'; BEGIN
 PERFORM pg_advisory_xact_lock(hashtextextended(current_schema()||':trust-company:'||NEW.company_id::text,0));
 NEW.risk_fingerprint=trust_listing_fingerprint(NEW.title,NEW.description||'|'||COALESCE(NEW.benefits,'')||'|'||COALESCE(NEW.prerequisites,'')||'|'||COALESCE(NEW.salary,'')||'|'||NEW.contact_email||'|'||COALESCE(NEW.accepted_courses,''),NEW.requirements,NEW.area,NEW.location,NEW.url);
 IF TG_OP='UPDATE' THEN NEW.trust_revision=OLD.trust_revision+1; END IF;
 IF NEW.kind='job' AND NEW.risk_approved_fingerprint IS DISTINCT FROM NEW.risk_fingerprint THEN
 body=geo_normalize(NEW.title||' '||NEW.description||' '||NEW.requirements||' '||COALESCE(NEW.benefits,'')||' '||COALESCE(NEW.prerequisites,''));
 IF body~'(taxa de inscricao|pague para participar|deposito antecipado|pague para se candidatar)' THEN signals=signals||'"CANDIDATE_PAYMENT_REQUEST"'::jsonb;END IF;
 IF body~'(envie sua senha|senha bancaria|codigo de autenticacao|codigo do cartao|cvv)' THEN signals=signals||'"SENSITIVE_CREDENTIAL_REQUEST"'::jsonb;END IF;
 IF COALESCE(NEW.url,'')~*'^https?://(www[.])?(bit[.]ly|tinyurl[.]com)/' THEN signals=signals||'"SHORTENED_EXTERNAL_LINK"'::jsonb;END IF;
 IF length(NEW.description)>=80 AND EXISTS(SELECT 1 FROM company_listings l WHERE l.id<>NEW.id AND l.company_id=NEW.company_id AND l.kind='job' AND l.risk_fingerprint=NEW.risk_fingerprint AND l.publication_status NOT IN ('CLOSED') AND l.created_at>CURRENT_TIMESTAMP-INTERVAL '30 days') THEN signals=signals||'"DUPLICATE_COMPANY_LISTING"'::jsonb;END IF;
 IF NEW.publication_status='PUBLISHED' AND (TG_OP='INSERT' OR OLD.publication_status IS DISTINCT FROM 'PUBLISHED') AND (SELECT count(*) FROM company_listing_publish_events WHERE company_id=NEW.company_id AND created_at>CURRENT_TIMESTAMP-INTERVAL '1 hour')>=10 THEN signals=signals||'"HIGH_PUBLICATION_VELOCITY"'::jsonb;END IF;
 END IF;
 NEW.risk_signals=signals;
 IF jsonb_array_length(signals)>0 AND NEW.risk_state<>'HELD' THEN NEW.risk_state='REVIEW';END IF;
 IF NEW.risk_state<>'CLEAR' AND NEW.publication_status='PUBLISHED' THEN NEW.publication_status='PAUSED';END IF;
 RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_trust_listing_guard BEFORE INSERT OR UPDATE ON company_listings FOR EACH ROW EXECUTE FUNCTION trust_listing_guard();
CREATE FUNCTION trust_listing_event() RETURNS trigger AS $$ BEGIN
 IF NEW.publication_status='PUBLISHED' AND (TG_OP='INSERT' OR OLD.publication_status IS DISTINCT FROM 'PUBLISHED') THEN INSERT INTO company_listing_publish_events(company_id,listing_id) VALUES (NEW.company_id,NEW.id);END IF;
 IF (TG_OP='INSERT' AND NEW.risk_state<>'CLEAR') OR (TG_OP='UPDATE' AND NEW.risk_state IS DISTINCT FROM OLD.risk_state) THEN
 INSERT INTO company_trust_audit(company_id,listing_id,actor,action,reason) VALUES (NEW.company_id,NEW.id,'AUTOMATED_RULES','LISTING_RISK_CHANGED','Sinalização revisável; não constitui confirmação de fraude.');
 INSERT INTO communication_events(source_key,kind,resource_id,scope_id) VALUES ('trust:listing:'||NEW.id||':'||NEW.trust_revision,'TRUST_REVIEW',NEW.id,NEW.company_id) ON CONFLICT DO NOTHING;
 END IF;RETURN NEW;END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_trust_listing_event AFTER INSERT OR UPDATE ON company_listings FOR EACH ROW EXECUTE FUNCTION trust_listing_event();
