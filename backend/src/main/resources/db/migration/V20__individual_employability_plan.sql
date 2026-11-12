-- Oportunidades only. Existing diagnostic, Radar, catalog and credentials are preserved.
CREATE TABLE individual_employability_plans (
 user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
 objectives JSONB NOT NULL, revision BIGINT NOT NULL DEFAULT 1 CHECK(revision>0),
 generation BIGINT NOT NULL DEFAULT 0 CHECK(generation>=0), snapshot JSONB,
 inputs_hash VARCHAR(64),generated_at TIMESTAMPTZ,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE individual_employability_steps (
 user_id BIGINT NOT NULL REFERENCES individual_employability_plans(user_id) ON DELETE CASCADE,
 step_key VARCHAR(64) NOT NULL,criterion_hash VARCHAR(64) NOT NULL,
 state VARCHAR(24) NOT NULL DEFAULT 'NOT_STARTED' CHECK(state IN ('NOT_STARTED','IN_PROGRESS','DECLARED_DONE','DISMISSED')),
 active BOOLEAN NOT NULL DEFAULT TRUE,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,step_key)
);
CREATE INDEX idx_individual_plan_active_steps ON individual_employability_steps(user_id,active);
CREATE TABLE individual_employability_runs (
 user_id BIGINT NOT NULL REFERENCES individual_employability_plans(user_id) ON DELETE CASCADE,
 request_key UUID NOT NULL,request_hash VARCHAR(64) NOT NULL,generation BIGINT NOT NULL,
 produced_revision BIGINT NOT NULL,snapshot JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,request_key),UNIQUE(user_id,generation)
);
CREATE INDEX idx_individual_plan_history ON individual_employability_runs(user_id,generation DESC);
CREATE INDEX idx_radar_audit_plan_quota ON radar_audit(actor_id,event,created_at);
