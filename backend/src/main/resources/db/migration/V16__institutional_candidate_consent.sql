CREATE TABLE hub_candidate_consents (
 user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,organization_id BIGINT NOT NULL CHECK(organization_id>0),
 institution_name VARCHAR(180) NOT NULL,consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(user_id,organization_id)
);
CREATE INDEX idx_hub_candidate_cohort ON hub_candidate_consents(organization_id,user_id);
