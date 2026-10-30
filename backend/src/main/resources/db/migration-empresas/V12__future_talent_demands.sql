CREATE TABLE company_future_demands (
 id BIGSERIAL PRIMARY KEY, company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
 occupation VARCHAR(180) NOT NULL, description VARCHAR(5000) NOT NULL, area VARCHAR(120) NOT NULL,
 estimated_hires INTEGER NOT NULL CHECK(estimated_hires BETWEEN 1 AND 10000),
 education_description VARCHAR(1000) NOT NULL DEFAULT '', experience_description VARCHAR(1000) NOT NULL DEFAULT '',
 region VARCHAR(180) NOT NULL, modality VARCHAR(40) NOT NULL CHECK(modality IN ('Presencial','Híbrido','Remoto','Online')),
 hiring_start DATE NOT NULL, hiring_end DATE NOT NULL CHECK(hiring_end>=hiring_start),
 status VARCHAR(16) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED','SUSPENDED','ARCHIVED')),
 revision INTEGER NOT NULL DEFAULT 1,
 requirements JSONB NOT NULL DEFAULT '{"revision":0,"skills":[],"experienceMonths":null,"educationLevel":null,"experienceEliminatory":false,"educationEliminatory":false,"city":"","modality":"","area":""}',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_future_demands_company ON company_future_demands(company_id,id DESC);
CREATE INDEX idx_future_demands_public ON company_future_demands(id DESC) WHERE status='PUBLISHED';
CREATE TABLE company_future_job_links (
 id BIGSERIAL PRIMARY KEY, demand_id BIGINT NOT NULL REFERENCES company_future_demands(id) ON DELETE CASCADE,
 listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,
 linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(demand_id,listing_id)
);
CREATE INDEX idx_future_job_links_demand ON company_future_job_links(demand_id,id);
