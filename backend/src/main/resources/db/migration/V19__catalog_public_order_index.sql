-- Additive. Large installations should apply in a planned maintenance window.
SET LOCAL lock_timeout='5s';
CREATE INDEX idx_catalog_public_order ON catalog_items(kind,created_at DESC,id DESC) WHERE active=TRUE AND moderation_status='APPROVED';
