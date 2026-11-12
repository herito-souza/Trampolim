-- Avoid a full event scan for each listing/indicator. No tenant data copied.
SET LOCAL lock_timeout='5s';
CREATE INDEX idx_listing_event_type ON company_listing_events(listing_id,event_type);
CREATE INDEX idx_listing_company_page ON company_listings(company_id,created_at DESC,id DESC);
