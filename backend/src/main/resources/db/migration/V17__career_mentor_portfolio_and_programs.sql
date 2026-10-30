CREATE TABLE career_ai_requests (
 id UUID PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 client_key UUID NOT NULL,request_hash VARCHAR(64) NOT NULL,topic VARCHAR(20) NOT NULL,
 status VARCHAR(16) NOT NULL CHECK(status IN ('RESERVED','SUCCEEDED','FAILED','UNKNOWN')),
 reserved_tokens INTEGER NOT NULL CHECK(reserved_tokens>0),input_tokens INTEGER,output_tokens INTEGER,
 estimated_usd NUMERIC(14,8),result JSONB,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(user_id,client_key)
);
CREATE INDEX idx_career_ai_daily ON career_ai_requests(created_at,user_id);
CREATE TABLE career_interview_sessions (
 id UUID PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 area VARCHAR(80) NOT NULL,occupation VARCHAR(180) NOT NULL,listing_id BIGINT,
 questions JSONB NOT NULL,answers JSONB NOT NULL DEFAULT '{}',feedback JSONB NOT NULL DEFAULT '{}',
 revision INTEGER NOT NULL DEFAULT 0,completed_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_career_interviews_owner ON career_interview_sessions(user_id,created_at DESC);
CREATE TABLE career_challenge_submissions (
 receipt_id UUID PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 challenge_id BIGINT NOT NULL,alias VARCHAR(80) NOT NULL,body VARCHAR(12000) NOT NULL,project_url VARCHAR(1000) NOT NULL DEFAULT '',
 request_hash VARCHAR(64) NOT NULL,consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,withdrawn_at TIMESTAMPTZ,
 UNIQUE(user_id,challenge_id)
);
CREATE INDEX idx_career_submissions_owner ON career_challenge_submissions(user_id,consent_at DESC);
CREATE TABLE career_delivery_jobs (
 id UUID PRIMARY KEY,owner_user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,receipt_id UUID NOT NULL,
 event_kind VARCHAR(16) NOT NULL CHECK(event_kind IN ('SUBMIT','WITHDRAW')),
 payload JSONB NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','DELIVERED','REJECTED')),
 attempts INTEGER NOT NULL DEFAULT 0,next_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(receipt_id,event_kind)
);
CREATE INDEX idx_career_delivery_pending ON career_delivery_jobs(next_at) WHERE status='PENDING';
CREATE FUNCTION career_erase_account() RETURNS trigger AS $$ BEGIN
 INSERT INTO career_delivery_jobs(id,owner_user_id,receipt_id,event_kind,payload)
 SELECT gen_random_uuid(),NULL,receipt_id,'WITHDRAW',jsonb_build_object('version',1,'receiptId',receipt_id) FROM career_challenge_submissions WHERE user_id=OLD.id
 ON CONFLICT(receipt_id,event_kind) DO NOTHING;
 UPDATE career_delivery_jobs SET payload='{}',status='REJECTED' WHERE owner_user_id=OLD.id AND event_kind='SUBMIT';
 RETURN OLD; END $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_career_erase_account BEFORE DELETE ON app_users FOR EACH ROW EXECUTE FUNCTION career_erase_account();
CREATE TABLE career_portfolio_items (
 id UUID PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 kind VARCHAR(24) NOT NULL CHECK(kind IN ('PROJECT','EXPERIENCE','COURSE','CHALLENGE')),
 title VARCHAR(180) NOT NULL,description VARCHAR(4000) NOT NULL,url VARCHAR(1000) NOT NULL DEFAULT '',submission_receipt UUID REFERENCES career_challenge_submissions(receipt_id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,CHECK((kind='CHALLENGE')=(submission_receipt IS NOT NULL))
);
CREATE INDEX idx_career_portfolio_owner ON career_portfolio_items(user_id,created_at DESC);
CREATE TABLE career_portfolio_shares (
 id UUID PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,recipient_email VARCHAR(180) NOT NULL,alias VARCHAR(80) NOT NULL DEFAULT '',
 item_ids JSONB NOT NULL,evidence_receipts JSONB NOT NULL,expires_at TIMESTAMPTZ NOT NULL,revoked_at TIMESTAMPTZ,consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_career_portfolio_shares_owner ON career_portfolio_shares(user_id,consent_at DESC);
CREATE TABLE career_first_job_steps (
 user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,step VARCHAR(24) NOT NULL CHECK(step IN ('RESUME_PLAN','TRAINING_PLAN','APPLICATION_PLAN')),
 notes VARCHAR(1000) NOT NULL DEFAULT '',updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(user_id,step)
);
CREATE TABLE career_observatory_consents (
 user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,region_code VARCHAR(2) NOT NULL,consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_career_region_consent ON career_observatory_consents(region_code,user_id);
CREATE TABLE career_satisfaction (
 user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5),updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE career_audit (
 id BIGSERIAL PRIMARY KEY,user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,event VARCHAR(48) NOT NULL,reference UUID,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_career_audit_owner ON career_audit(user_id,created_at DESC);
