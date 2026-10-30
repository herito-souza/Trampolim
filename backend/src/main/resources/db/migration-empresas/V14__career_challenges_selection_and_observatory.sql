CREATE TABLE company_challenges (
 id BIGSERIAL PRIMARY KEY,company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
 title VARCHAR(180) NOT NULL,description VARCHAR(5000) NOT NULL,skills JSONB NOT NULL,rubric JSONB NOT NULL,
 materials VARCHAR(3000) NOT NULL DEFAULT '',material_url VARCHAR(1000) NOT NULL DEFAULT '',terms VARCHAR(3000) NOT NULL,
 deadline TIMESTAMPTZ NOT NULL,status VARCHAR(16) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED','CLOSED')),
 revision INTEGER NOT NULL DEFAULT 1,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_company_challenge_owner ON company_challenges(company_id,id DESC);
CREATE INDEX idx_company_challenge_public ON company_challenges(id DESC) WHERE status='PUBLISHED';
CREATE TABLE company_challenge_submissions (
 receipt_id UUID PRIMARY KEY,challenge_id BIGINT NOT NULL REFERENCES company_challenges(id) ON DELETE CASCADE,
 alias VARCHAR(80) NOT NULL,body VARCHAR(12000) NOT NULL,project_url VARCHAR(1000) NOT NULL DEFAULT '',request_hash VARCHAR(64) NOT NULL,
 rubric JSONB NOT NULL,consent_at TIMESTAMPTZ NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'RECEIVED' CHECK(status IN ('RECEIVED','REVIEWED','WITHDRAWN')),
 received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,withdrawn_at TIMESTAMPTZ
);
CREATE INDEX idx_company_challenge_inbox ON company_challenge_submissions(challenge_id,received_at DESC);
CREATE TABLE company_challenge_reviews (
 id UUID PRIMARY KEY,receipt_id UUID NOT NULL UNIQUE REFERENCES company_challenge_submissions(receipt_id) ON DELETE CASCADE,
 reviewer_id BIGINT NOT NULL REFERENCES company_users(id),scores JSONB NOT NULL,score_percent NUMERIC(6,2) NOT NULL CHECK(score_percent BETWEEN 0 AND 100),
 feedback VARCHAR(3000) NOT NULL,reviewed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,revoked_at TIMESTAMPTZ
);
CREATE TABLE company_career_context (
 listing_id BIGINT PRIMARY KEY REFERENCES company_listings(id) ON DELETE CASCADE,
 regime VARCHAR(20) NOT NULL CHECK(regime IN ('EMPLOYMENT','INTERNSHIP','APPRENTICESHIP')),
 region_code VARCHAR(2) NOT NULL,school_requirements VARCHAR(1000) NOT NULL DEFAULT '',institution VARCHAR(180) NOT NULL DEFAULT '',
 documentation VARCHAR(2000) NOT NULL DEFAULT '',first_experience BOOLEAN NOT NULL DEFAULT FALSE,
 employer_legal_confirmation BOOLEAN NOT NULL DEFAULT FALSE,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_company_career_context_region ON company_career_context(region_code,regime,listing_id);
CREATE TABLE career_listing_publications (listing_id BIGINT PRIMARY KEY REFERENCES company_listings(id) ON DELETE CASCADE,published_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE FUNCTION career_track_publication() RETURNS trigger AS $$ BEGIN
 IF NEW.kind='job' AND NEW.publication_status='PUBLISHED' AND (TG_OP='INSERT' OR OLD.publication_status<>'PUBLISHED') THEN
 INSERT INTO career_listing_publications(listing_id) VALUES (NEW.id) ON CONFLICT DO NOTHING; END IF; RETURN NEW; END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_career_listing_publication AFTER INSERT OR UPDATE OF publication_status ON company_listings FOR EACH ROW EXECUTE FUNCTION career_track_publication();
CREATE TABLE company_selection_outcomes (
 id UUID PRIMARY KEY,application_id BIGINT NOT NULL UNIQUE REFERENCES company_applications(id) ON DELETE CASCADE,
 interview_at TIMESTAMPTZ,interview_held BOOLEAN NOT NULL DEFAULT FALSE,hire_date DATE,retained_until DATE,confirmation_hash VARCHAR(64) UNIQUE,confirmation_expires_at TIMESTAMPTZ,
 candidate_confirmed_at TIMESTAMPTZ,public_metrics_consent BOOLEAN NOT NULL DEFAULT FALSE,request_key UUID NOT NULL,request_hash VARCHAR(64) NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,CHECK(retained_until IS NULL OR hire_date IS NOT NULL AND retained_until>=hire_date)
);
CREATE INDEX idx_selection_outcome_confirmed ON company_selection_outcomes(candidate_confirmed_at) WHERE candidate_confirmed_at IS NOT NULL;
CREATE TABLE company_operating_costs (
 company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,month DATE NOT NULL,
 amount_cents BIGINT NOT NULL CHECK(amount_cents BETWEEN 0 AND 100000000000),source VARCHAR(500) NOT NULL,recorded_by VARCHAR(180) NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(company_id,month),CHECK(extract(day FROM month)=1)
);

CREATE TABLE career_withdrawn_receipts(receipt_id UUID PRIMARY KEY,withdrawn_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);

CREATE TABLE career_observatory_daily (
 region_code VARCHAR(2) NOT NULL,day DATE NOT NULL DEFAULT CURRENT_DATE,open_jobs BIGINT NOT NULL,future_demands BIGINT NOT NULL,
 observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(region_code,day)
);
