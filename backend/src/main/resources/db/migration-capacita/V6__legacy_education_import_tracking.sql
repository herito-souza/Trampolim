-- Preserve source identities so a legacy catalog can be imported repeatedly without duplicates.
ALTER TABLE education_tracks ADD COLUMN legacy_track_id BIGINT;
ALTER TABLE education_categories ADD COLUMN legacy_category_id BIGINT;
ALTER TABLE education_contents ADD COLUMN legacy_content_id BIGINT;

CREATE UNIQUE INDEX uq_education_tracks_legacy_id ON education_tracks(legacy_track_id) WHERE legacy_track_id IS NOT NULL;
CREATE UNIQUE INDEX uq_education_categories_legacy_id ON education_categories(legacy_category_id) WHERE legacy_category_id IS NOT NULL;
CREATE UNIQUE INDEX uq_education_contents_legacy_id ON education_contents(legacy_content_id) WHERE legacy_content_id IS NOT NULL;
