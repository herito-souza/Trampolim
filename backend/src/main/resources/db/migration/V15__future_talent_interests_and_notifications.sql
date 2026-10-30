ALTER TABLE radar_match_cache DROP CONSTRAINT radar_match_cache_source_check;
ALTER TABLE radar_match_cache ADD CONSTRAINT radar_match_cache_source_check CHECK(source IN ('catalog','company','future'));
CREATE TABLE future_talent_interests (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 demand_id BIGINT NOT NULL CHECK(demand_id>0), occupation VARCHAR(180) NOT NULL, area VARCHAR(120) NOT NULL,
 consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, notify BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, refresh_after TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(user_id,demand_id)
);
CREATE INDEX idx_future_interest_demand ON future_talent_interests(demand_id,user_id);
CREATE INDEX idx_future_interest_notify ON future_talent_interests(demand_id,user_id) WHERE notify=TRUE;
CREATE TABLE future_preparation_snapshots (
 interest_id BIGINT PRIMARY KEY REFERENCES future_talent_interests(id) ON DELETE CASCADE,
 requirements_revision INTEGER NOT NULL, policy_revision INTEGER NOT NULL,
 score NUMERIC(6,2), ready BOOLEAN NOT NULL, baseline_gaps JSONB NOT NULL, gaps JSONB NOT NULL,
 met JSONB NOT NULL, verified_met JSONB NOT NULL, checked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_future_preparation_refresh ON future_preparation_snapshots(checked_at,interest_id);
CREATE TABLE future_preparation_daily (
 interest_id BIGINT NOT NULL REFERENCES future_talent_interests(id) ON DELETE CASCADE,
 day DATE NOT NULL DEFAULT CURRENT_DATE, requirements_revision INTEGER NOT NULL, policy_revision INTEGER NOT NULL,
 score NUMERIC(6,2), ready BOOLEAN NOT NULL, gap_count INTEGER NOT NULL,
 PRIMARY KEY(interest_id,day)
);
CREATE TABLE future_training_plans (
 interest_id BIGINT NOT NULL REFERENCES future_talent_interests(id) ON DELETE CASCADE,
 course_id BIGINT NOT NULL, module_id BIGINT NOT NULL, course_title VARCHAR(180) NOT NULL, module_title VARCHAR(180) NOT NULL,
 status VARCHAR(16) NOT NULL CHECK(status IN ('IN_PROGRESS','STOPPED')),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(interest_id,course_id,module_id)
);
CREATE TABLE future_delivery_state (id INTEGER PRIMARY KEY CHECK(id=1),cursor BIGINT NOT NULL DEFAULT 0);
INSERT INTO future_delivery_state(id) VALUES(1);
CREATE TABLE future_notification_jobs (
 event_id BIGINT PRIMARY KEY, demand_id BIGINT NOT NULL, listing_id BIGINT NOT NULL,
 title VARCHAR(180) NOT NULL, published_at TIMESTAMPTZ NOT NULL,
 cursor_user BIGINT NOT NULL DEFAULT 0, completed BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_future_notification_work ON future_notification_jobs(event_id) WHERE completed=FALSE;
CREATE TABLE future_talent_notifications (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 demand_id BIGINT NOT NULL, listing_id BIGINT NOT NULL, title VARCHAR(180) NOT NULL,
 read_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id,demand_id,listing_id)
);
CREATE INDEX idx_future_notifications_owner ON future_talent_notifications(user_id,id DESC);
CREATE INDEX idx_future_notifications_unread ON future_talent_notifications(user_id) WHERE read_at IS NULL;
CREATE TABLE future_talent_audit (
 id BIGSERIAL PRIMARY KEY, actor_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
 event VARCHAR(40) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_future_interest_refresh ON future_talent_interests(refresh_after,id);
CREATE INDEX idx_future_daily_retention ON future_preparation_daily(day,interest_id);
