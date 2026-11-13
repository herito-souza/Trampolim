-- Educational organizations use the existing institutional Hub. Existing
-- records are kept pending review; no organization is verified by migration.
ALTER TABLE enterprise_organizations
    ADD COLUMN verification_status VARCHAR(24) NOT NULL DEFAULT 'NOT_APPLICABLE'
        CHECK (verification_status IN ('NOT_APPLICABLE','PENDING_REVIEW','VERIFIED','REJECTED')),
    ADD COLUMN verification_note VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD COLUMN verified_by VARCHAR(180);

UPDATE enterprise_organizations
   SET verification_status='PENDING_REVIEW'
 WHERE organization_type='EDUCATIONAL';

INSERT INTO hub_profiles(organization_id)
SELECT id FROM enterprise_organizations WHERE organization_type='EDUCATIONAL'
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION hub_initialize_profile() RETURNS trigger AS $$
BEGIN
 IF NEW.organization_type IN ('ASSOCIATION','EDUCATIONAL') THEN
  INSERT INTO hub_profiles(organization_id) VALUES (NEW.id) ON CONFLICT DO NOTHING;
  IF NEW.organization_type='EDUCATIONAL' AND NEW.verification_status='NOT_APPLICABLE' THEN
   UPDATE enterprise_organizations SET verification_status='PENDING_REVIEW' WHERE id=NEW.id;
  END IF;
 END IF;
 RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE INDEX idx_hub_educational_verification
    ON enterprise_organizations(verification_status,id)
    WHERE organization_type='EDUCATIONAL';
