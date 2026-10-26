CREATE TABLE IF NOT EXISTS company_commercial_interests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    offer_type VARCHAR(32) NOT NULL CHECK (offer_type IN ('PLAN', 'HIGHLIGHT', 'RECRUITMENT', 'COURSE_PARTNERSHIP', 'INSTITUTIONAL')),
    title VARCHAR(160) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_company_commercial_interests_company
    ON company_commercial_interests(company_id, created_at DESC);
