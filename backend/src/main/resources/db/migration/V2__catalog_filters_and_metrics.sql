ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS contract_type VARCHAR(40);
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS work_schedule VARCHAR(80);
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS last_validity_reminder_at TIMESTAMPTZ;
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS last_link_check_at TIMESTAMPTZ;
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS last_link_reminder_at TIMESTAMPTZ;
ALTER TABLE catalog_items ADD COLUMN IF NOT EXISTS link_status VARCHAR(20) DEFAULT 'UNKNOWN';

CREATE TABLE IF NOT EXISTS catalog_metrics (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(24) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    item_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_catalog_metrics_created_at ON catalog_metrics(created_at);
