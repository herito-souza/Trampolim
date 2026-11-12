CREATE TABLE observatory_epoch(id INTEGER PRIMARY KEY CHECK(id=1),revision BIGINT NOT NULL DEFAULT 1);
INSERT INTO observatory_epoch(id) VALUES (1);
CREATE TABLE observatory_runs(id UUID PRIMARY KEY,epoch BIGINT NOT NULL,observed_at TIMESTAMPTZ NOT NULL,coverage_from DATE NOT NULL,coverage_through DATE NOT NULL,quality JSONB NOT NULL DEFAULT '{}');
CREATE INDEX idx_observatory_runs_latest ON observatory_runs(observed_at DESC);
CREATE TABLE observatory_personal_cells(run_id UUID NOT NULL REFERENCES observatory_runs(id) ON DELETE CASCADE,metric VARCHAR(40) NOT NULL,month DATE NOT NULL,state VARCHAR(2) NOT NULL,value BIGINT NOT NULL CHECK(value>=0),contributors BIGINT NOT NULL CHECK(contributors>=0),organizations BIGINT NOT NULL DEFAULT 0,PRIMARY KEY(run_id,metric,month,state));
CREATE FUNCTION observatory_invalidate() RETURNS trigger AS $$ BEGIN UPDATE observatory_epoch SET revision=revision+1 WHERE id=1; RETURN NULL; END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_observatory_learning_privacy AFTER INSERT OR UPDATE OR DELETE ON learning_observatory_consents FOR EACH STATEMENT EXECUTE FUNCTION observatory_invalidate();
CREATE TRIGGER trg_observatory_learning_account AFTER UPDATE OF account_status,email_verified OR DELETE ON student_users FOR EACH STATEMENT EXECUTE FUNCTION observatory_invalidate();
CREATE TRIGGER trg_observatory_award_privacy AFTER UPDATE OF revoked_at OR DELETE ON learning_competency_awards FOR EACH STATEMENT EXECUTE FUNCTION observatory_invalidate();
CREATE INDEX idx_observatory_award_period ON learning_competency_awards(awarded_at,student_id) WHERE revoked_at IS NULL;
