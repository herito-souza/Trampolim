-- Existing challenges/answers/reviews remain the source of truth. Legacy publications require review.
ALTER TABLE company_challenges ADD COLUMN review_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK(review_status IN ('DRAFT','PENDING','APPROVED','REJECTED'));
ALTER TABLE company_challenges ADD COLUMN estimated_minutes INTEGER NOT NULL DEFAULT 30 CHECK(estimated_minutes BETWEEN 5 AND 120);
ALTER TABLE company_challenges ADD COLUMN participant_limit INTEGER NOT NULL DEFAULT 100 CHECK(participant_limit BETWEEN 1 AND 10000);
ALTER TABLE company_challenges ADD COLUMN safeguards_accepted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_challenge_moderation ON company_challenges(review_status,id DESC);
CREATE TABLE company_challenge_moderation (
 id BIGSERIAL PRIMARY KEY,challenge_id BIGINT NOT NULL REFERENCES company_challenges(id) ON DELETE CASCADE,
 actor VARCHAR(180) NOT NULL,action VARCHAR(24) NOT NULL,reason VARCHAR(2000) NOT NULL,revision INTEGER NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_challenge_moderation_history ON company_challenge_moderation(challenge_id,id DESC);
ALTER TABLE company_challenge_reviews ADD COLUMN revocation_reason VARCHAR(500);
ALTER TABLE company_challenge_submissions ADD COLUMN accepted_revision INTEGER;
