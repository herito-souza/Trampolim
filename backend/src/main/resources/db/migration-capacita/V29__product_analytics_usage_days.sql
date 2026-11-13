CREATE TABLE product_usage_days (
    user_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    usage_day DATE NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, usage_day)
);
CREATE INDEX idx_product_usage_days_period ON product_usage_days(usage_day, user_id);
CREATE INDEX idx_product_analytics_course_events ON domain_event_outbox(event_name, occurred_at) WHERE event_name='course.completed.v1';
