CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL UNIQUE REFERENCES company_users(id) ON DELETE CASCADE,
    legal_name VARCHAR(180) NOT NULL,
    trade_name VARCHAR(180) NOT NULL,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    phone VARCHAR(40) NOT NULL,
    website VARCHAR(255),
    industry VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_members (
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES company_users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'RECRUITER', 'ANALYST')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(company_id, user_id)
);

CREATE TABLE company_listings (
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
    is_internship BOOLEAN NOT NULL DEFAULT FALSE,
    accepted_courses VARCHAR(1000),
    headcount INTEGER,
    application_deadline DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_company_listings_company ON company_listings(company_id, created_at DESC);

CREATE TABLE company_listing_events (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,
    actor_email VARCHAR(180),
    event_type VARCHAR(40) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_listing_payments (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES company_listings(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO',
    external_reference VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key UUID NOT NULL UNIQUE,
    provider_order_id VARCHAR(80) UNIQUE,
    checkout_url VARCHAR(1000),
    amount_cents INTEGER NOT NULL CHECK (amount_cents = 5000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATING', 'PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED')),
    provider_status VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMPTZ
);
CREATE INDEX idx_company_listing_payments_history ON company_listing_payments(listing_id, created_at DESC);
CREATE UNIQUE INDEX uq_company_listing_one_open_payment ON company_listing_payments(listing_id) WHERE status IN ('CREATING', 'PENDING');

CREATE TABLE company_invites (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'RECRUITER', 'ANALYST')),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
