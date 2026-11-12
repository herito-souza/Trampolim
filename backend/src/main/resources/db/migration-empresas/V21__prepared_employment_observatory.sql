CREATE TABLE observatory_epoch(id INTEGER PRIMARY KEY CHECK(id=1),revision BIGINT NOT NULL DEFAULT 1);
INSERT INTO observatory_epoch(id) VALUES (1);
CREATE TABLE observatory_runs(id UUID PRIMARY KEY,epoch BIGINT NOT NULL,observed_at TIMESTAMPTZ NOT NULL,coverage_from DATE NOT NULL,coverage_through DATE NOT NULL,quality JSONB NOT NULL DEFAULT '{}');
CREATE INDEX idx_observatory_runs_latest ON observatory_runs(observed_at DESC);
CREATE TABLE observatory_personal_cells(run_id UUID NOT NULL REFERENCES observatory_runs(id) ON DELETE CASCADE,metric VARCHAR(40) NOT NULL,month DATE NOT NULL,state VARCHAR(2) NOT NULL,value BIGINT NOT NULL CHECK(value>=0),contributors BIGINT NOT NULL CHECK(contributors>=0),organizations BIGINT NOT NULL DEFAULT 0,PRIMARY KEY(run_id,metric,month,state));
CREATE FUNCTION observatory_invalidate() RETURNS trigger AS $$ BEGIN UPDATE observatory_epoch SET revision=revision+1 WHERE id=1; RETURN NULL; END $$ LANGUAGE plpgsql;

CREATE TABLE observatory_municipalities(code VARCHAR(7) PRIMARY KEY,name VARCHAR(180) NOT NULL,state VARCHAR(2) NOT NULL);
CREATE TABLE observatory_vacancy_cells(run_id UUID NOT NULL REFERENCES observatory_runs(id) ON DELETE CASCADE,month DATE NOT NULL,municipality_code VARCHAR(7) NOT NULL DEFAULT '',state VARCHAR(2) NOT NULL DEFAULT '',area VARCHAR(180) NOT NULL DEFAULT '',published BIGINT NOT NULL DEFAULT 0,active BIGINT NOT NULL DEFAULT 0,untracked_active BIGINT NOT NULL DEFAULT 0,PRIMARY KEY(run_id,month,municipality_code,state,area));
CREATE INDEX idx_observatory_vacancy_scope ON observatory_vacancy_cells(run_id,state,month,municipality_code);
CREATE TABLE observatory_company_presence(run_id UUID NOT NULL REFERENCES observatory_runs(id) ON DELETE CASCADE,month DATE NOT NULL,municipality_code VARCHAR(7) NOT NULL DEFAULT '',state VARCHAR(2) NOT NULL DEFAULT '',company_id BIGINT NOT NULL,PRIMARY KEY(run_id,month,municipality_code,state,company_id));
CREATE INDEX idx_observatory_presence_scope ON observatory_company_presence(run_id,state,month,municipality_code);
CREATE TRIGGER trg_observatory_outcome_privacy AFTER UPDATE OF public_metrics_consent OR DELETE ON company_selection_outcomes FOR EACH STATEMENT EXECUTE FUNCTION observatory_invalidate();
CREATE TRIGGER trg_observatory_application_privacy AFTER DELETE OR UPDATE OF candidate_email ON company_applications FOR EACH STATEMENT EXECUTE FUNCTION observatory_invalidate();
CREATE INDEX idx_observatory_publication_time ON career_listing_publications(published_at,listing_id);
CREATE INDEX idx_observatory_application_time ON company_applications(created_at,listing_id);
CREATE INDEX idx_observatory_interview_time ON company_selection_outcomes(interview_at,application_id) WHERE interview_at IS NOT NULL;
CREATE INDEX idx_observatory_confirmation_time ON company_selection_outcomes(candidate_confirmed_at,application_id) WHERE candidate_confirmed_at IS NOT NULL AND public_metrics_consent=TRUE;
