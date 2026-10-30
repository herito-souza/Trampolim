-- Cursor feed; dates are intentionally checked at read time, never embedded as a volatile predicate.
CREATE INDEX idx_enterprise_internships_public_cursor ON company_listings(id DESC)
    WHERE kind='job' AND is_internship=TRUE AND payment_status='PAID' AND publication_status='PUBLISHED';
