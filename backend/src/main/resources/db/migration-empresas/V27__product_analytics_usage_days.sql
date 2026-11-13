CREATE TABLE product_usage_days (
    user_id BIGINT NOT NULL REFERENCES company_users(id) ON DELETE CASCADE,
    usage_day DATE NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, usage_day)
);
CREATE INDEX idx_product_usage_days_period ON product_usage_days(usage_day, user_id);
CREATE INDEX idx_product_analytics_applicant_first ON company_applications(lower(btrim(candidate_email)), created_at, id) WHERE consent_at IS NOT NULL;
CREATE INDEX idx_product_analytics_hire_date ON company_selection_outcomes(hire_date, application_id) WHERE candidate_confirmed_at IS NOT NULL AND hire_date IS NOT NULL AND public_metrics_consent=TRUE;
