-- The candidate center looks up only submissions linked to a verified Oportunidades email.
CREATE INDEX idx_company_applications_candidate_email_center
 ON company_applications (LOWER(BTRIM(candidate_email)), created_at DESC, id DESC)
 WHERE consent_at IS NOT NULL;
