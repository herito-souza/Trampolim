CREATE TABLE product_usage_days (
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    usage_day DATE NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, usage_day)
);
CREATE INDEX idx_product_usage_days_period ON product_usage_days(usage_day, user_id);
CREATE INDEX idx_product_analytics_accounts_created ON app_users(created_at) WHERE account_type='PERSON';
