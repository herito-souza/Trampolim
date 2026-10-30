CREATE TABLE company_radar_requirements (
 listing_id BIGINT PRIMARY KEY REFERENCES company_listings(id) ON DELETE CASCADE,
 revision INTEGER NOT NULL DEFAULT 1, definition JSONB NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_company_radar_public_jobs ON company_listings(id DESC) WHERE kind='job' AND publication_status='PUBLISHED';
