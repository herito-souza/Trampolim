-- Only new metadata and a guard; no existing personal records are removed.
CREATE TABLE privacy_requests (
 id UUID PRIMARY KEY,
 user_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 client_key UUID NOT NULL,
 state VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','REVIEWING','READY','RETAINED','COMPLETED','CANCELLED')),
 basis_code VARCHAR(40) NOT NULL DEFAULT 'USER_REQUEST' CHECK(basis_code IN ('USER_REQUEST','ORGANIZATION_RESPONSIBILITY','RETENTION_REVIEW','SECURITY_REVIEW','SELF_SERVICE_DELETION')),
 case_reference VARCHAR(80),
 review_after TIMESTAMPTZ,
 revision BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id,client_key)
);
CREATE UNIQUE INDEX privacy_one_active_request ON privacy_requests(user_id) WHERE state IN ('OPEN','REVIEWING','READY','RETAINED');
CREATE INDEX privacy_review_queue ON privacy_requests(state,updated_at,id);
CREATE TABLE privacy_audit (
 id BIGSERIAL PRIMARY KEY,
 request_id UUID NOT NULL REFERENCES privacy_requests(id),
 actor_ref VARCHAR(80),
 event VARCHAR(40) NOT NULL,
 from_state VARCHAR(20),to_state VARCHAR(20),basis_code VARCHAR(40),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX privacy_audit_request ON privacy_audit(request_id,id);
CREATE FUNCTION privacy_guard_account_delete() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM privacy_requests WHERE user_id=OLD.id AND state IN ('REVIEWING','RETAINED')) THEN
  RAISE EXCEPTION USING ERRCODE='23514',CONSTRAINT='privacy_account_review',MESSAGE='Account deletion requires privacy review';
 END IF;
 INSERT INTO privacy_audit(request_id,event,from_state,to_state,basis_code)
 SELECT id,'ACCOUNT_DELETED',state,'COMPLETED','SELF_SERVICE_DELETION' FROM privacy_requests
 WHERE user_id=OLD.id AND state IN ('OPEN','READY');
 UPDATE privacy_requests SET state='COMPLETED',basis_code='SELF_SERVICE_DELETION',case_reference=NULL,review_after=NULL,revision=revision+1,updated_at=CURRENT_TIMESTAMP
 WHERE user_id=OLD.id AND state IN ('OPEN','READY');
 UPDATE privacy_audit SET actor_ref=NULL WHERE actor_ref='holder:'||OLD.id;
 RETURN OLD;
END $$;
CREATE TRIGGER privacy_account_delete_guard BEFORE DELETE ON student_users FOR EACH ROW EXECUTE FUNCTION privacy_guard_account_delete();

CREATE TABLE privacy_export_audit (
 id BIGSERIAL PRIMARY KEY,
 user_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 event VARCHAR(32) NOT NULL CHECK(event='EXPORT_REQUESTED'),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX privacy_export_user_date ON privacy_export_audit(user_id,created_at);
