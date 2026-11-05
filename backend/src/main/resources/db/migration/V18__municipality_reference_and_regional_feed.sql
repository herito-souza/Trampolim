CREATE FUNCTION geo_normalize(value TEXT) RETURNS TEXT LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS $$
 SELECT trim(regexp_replace(translate(lower(COALESCE(value,'')), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn'), '[^a-z0-9]+', ' ', 'g'))
$$;
ALTER TABLE catalog_items ADD COLUMN municipality_code VARCHAR(7) CHECK(municipality_code IS NULL OR municipality_code ~ '^[0-9]{7}$');
CREATE INDEX idx_catalog_municipal_feed ON catalog_items(kind,municipality_code,id DESC) WHERE active=TRUE AND moderation_status='APPROVED';
CREATE INDEX idx_catalog_legacy_place ON catalog_items(geo_normalize(location),id DESC) WHERE active=TRUE AND moderation_status='APPROVED';
