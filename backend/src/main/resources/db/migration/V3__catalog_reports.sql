CREATE TABLE IF NOT EXISTS catalog_reports (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    reporter_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    reason VARCHAR(32) NOT NULL,
    details VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    reviewed_by VARCHAR(180)
);

CREATE INDEX IF NOT EXISTS idx_catalog_reports_status_created ON catalog_reports(status, created_at);
CREATE INDEX IF NOT EXISTS idx_catalog_reports_reporter_item ON catalog_reports(reporter_user_id, item_id, status);
