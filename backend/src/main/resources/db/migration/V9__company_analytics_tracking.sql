ALTER TABLE company_listing_events DROP CONSTRAINT IF EXISTS company_listing_events_event_type_check;
ALTER TABLE company_listing_events
    ADD CONSTRAINT company_listing_events_event_type_check
    CHECK (event_type IN ('VIEW', 'LISTING_VIEW', 'APPLICATION_CLICK'));

-- The old VIEW event was written for every listing whenever the company page opened,
-- even for listings outside the viewport. Discard those misleading historical counts.
DELETE FROM company_listing_events WHERE event_type = 'VIEW';

ALTER TABLE company_commercial_interests DROP CONSTRAINT IF EXISTS company_commercial_interests_offer_type_check;
ALTER TABLE company_commercial_interests
    ADD CONSTRAINT company_commercial_interests_offer_type_check
    CHECK (offer_type IN ('PLAN', 'HIGHLIGHT', 'RECRUITMENT', 'COURSE_PARTNERSHIP', 'INSTITUTIONAL', 'ANALYTICS'));
