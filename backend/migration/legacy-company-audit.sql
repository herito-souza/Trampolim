-- Read-only inventory of the legacy Empresas data in the Oportunidades database.
-- Run before planning or executing any transfer; this file performs no writes.
SELECT 'company_accounts' AS dataset, COUNT(*) AS records
FROM app_users WHERE account_type = 'COMPANY'
UNION ALL
SELECT 'companies', COUNT(*) FROM companies
UNION ALL
SELECT 'company_members', COUNT(*) FROM company_members
UNION ALL
SELECT 'company_listings', COUNT(*) FROM company_listings
UNION ALL
SELECT 'company_invites', COUNT(*) FROM company_invites
UNION ALL
SELECT 'company_listing_events', COUNT(*) FROM company_listing_events
UNION ALL
SELECT 'company_applications', COUNT(*) FROM company_applications
ORDER BY dataset;

-- Accounts that have legacy company records but an unexpected/missing type.
SELECT c.id AS company_id, u.id AS user_id, u.email, u.account_type
FROM companies c
JOIN app_users u ON u.id = c.user_id
WHERE u.account_type IS DISTINCT FROM 'COMPANY'
ORDER BY c.id;

-- An email that differs only by case must be reconciled before importing into
-- the normalized independent account tables.
SELECT LOWER(u.email) AS normalized_email, COUNT(*) AS account_count,
       ARRAY_AGG(DISTINCT u.email ORDER BY u.email) AS source_emails
FROM companies c
JOIN app_users u ON u.id = c.user_id
GROUP BY LOWER(u.email)
HAVING COUNT(*) > 1
ORDER BY normalized_email;

-- Legacy publication/payment statuses are evidence only. Never use these values
-- alone to republish a listing or mark a charge paid in the new database.
SELECT l.publication_status, l.payment_status, COUNT(*) AS listings
FROM company_listings l
GROUP BY l.publication_status, l.payment_status
ORDER BY l.publication_status, l.payment_status;

-- V11 was an uncommitted local addition and may not exist in every legacy DB.
SELECT to_regclass('public.company_listing_payments') IS NOT NULL AS legacy_payment_table_exists;
