CREATE FUNCTION geo_normalize(value TEXT) RETURNS TEXT LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS $$
 SELECT trim(regexp_replace(translate(lower(COALESCE(value,'')), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn'), '[^a-z0-9]+', ' ', 'g'))
$$;
ALTER TABLE company_listings ADD COLUMN municipality_code VARCHAR(7) CHECK(municipality_code IS NULL OR municipality_code ~ '^[0-9]{7}$');
CREATE INDEX idx_company_municipal_feed ON company_listings(municipality_code,id DESC) WHERE kind='job' AND publication_status='PUBLISHED';
CREATE INDEX idx_company_legacy_place ON company_listings(geo_normalize(location),id DESC) WHERE kind='job' AND publication_status='PUBLISHED';
