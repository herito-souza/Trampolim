ALTER TABLE app_users ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) NOT NULL DEFAULT 'PERSON';

CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    legal_name VARCHAR(180) NOT NULL,
    trade_name VARCHAR(180) NOT NULL,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    phone VARCHAR(40) NOT NULL,
    website VARCHAR(255),
    industry VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS company_listings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('job', 'course')),
    title VARCHAR(180) NOT NULL,
    area VARCHAR(80) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    requirements VARCHAR(3000) NOT NULL,
    location VARCHAR(180) NOT NULL,
    modality VARCHAR(40) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    contact_email VARCHAR(180) NOT NULL,
    salary VARCHAR(100),
    contract_type VARCHAR(80),
    work_schedule VARCHAR(100),
    workload VARCHAR(100),
    prerequisites VARCHAR(2000),
    benefits VARCHAR(2000),
    course_price VARCHAR(100),
    certificate VARCHAR(200),
    price_cents INTEGER NOT NULL DEFAULT 10000,
    payment_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    publication_status VARCHAR(24) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_company_listings_company ON company_listings(company_id, created_at DESC);
