CREATE TABLE skill_concepts (
 code VARCHAR(64) PRIMARY KEY CHECK(code ~ '^[a-z][a-z0-9-]{1,63}$'),
 name VARCHAR(180) NOT NULL, description VARCHAR(2000) NOT NULL,
 category VARCHAR(80) NOT NULL, kind VARCHAR(16) NOT NULL CHECK(kind IN ('TECHNICAL','BEHAVIORAL')),
 active BOOLEAN NOT NULL DEFAULT TRUE, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE skill_terms (
 term VARCHAR(180) PRIMARY KEY, skill_code VARCHAR(64) NOT NULL REFERENCES skill_concepts(code),
 display_name VARCHAR(180) NOT NULL
);
CREATE INDEX idx_skill_terms_code ON skill_terms(skill_code);
INSERT INTO skill_concepts(code,name,description,category,kind) VALUES
 ('excel','Planilhas eletrônicas','Organizar dados, fórmulas e análises em planilhas.','Ferramentas digitais','TECHNICAL'),
 ('text-editing','Edição de textos','Produzir e formatar documentos digitais.','Ferramentas digitais','TECHNICAL'),
 ('digital-literacy','Informática básica','Utilizar arquivos, sistemas e ferramentas digitais.','Ferramentas digitais','TECHNICAL'),
 ('sql','SQL','Consultar e organizar dados relacionais.','Tecnologia','TECHNICAL'),
 ('java','Java','Desenvolver software utilizando Java.','Tecnologia','TECHNICAL'),
 ('python','Python','Programar e automatizar tarefas com Python.','Tecnologia','TECHNICAL'),
 ('data-analysis','Análise de dados','Interpretar dados e comunicar resultados.','Tecnologia','TECHNICAL'),
 ('communication','Comunicação','Transmitir e compreender informações com clareza.','Comunicação','BEHAVIORAL'),
 ('teamwork','Trabalho em equipe','Colaborar e compartilhar responsabilidades.','Colaboração','BEHAVIORAL'),
 ('problem-solving','Resolução de problemas','Identificar causas e construir soluções.','Organização','BEHAVIORAL'),
 ('time-management','Organização do tempo','Planejar atividades e acompanhar prazos.','Organização','BEHAVIORAL'),
 ('customer-service','Atendimento ao cliente','Atender necessidades e orientar clientes.','Atendimento','TECHNICAL'),
 ('professional-writing','Redação profissional','Produzir textos claros para situações profissionais.','Comunicação','TECHNICAL');
INSERT INTO skill_terms(term,skill_code,display_name) VALUES
 ('planilhas eletronicas','excel','Planilhas eletrônicas'),('excel','excel','Excel'),('microsoft excel','excel','Microsoft Excel'),('google sheets','excel','Google Sheets'),
 ('edicao de textos','text-editing','Edição de textos'),('word','text-editing','Word'),
 ('informatica basica','digital-literacy','Informática básica'),('letramento digital','digital-literacy','Letramento digital'),
 ('sql','sql','SQL'),('java','java','Java'),('python','python','Python'),
 ('analise de dados','data-analysis','Análise de dados'),('comunicacao','communication','Comunicação'),
 ('trabalho em equipe','teamwork','Trabalho em equipe'),('colaboracao','teamwork','Colaboração'),
 ('resolucao de problemas','problem-solving','Resolução de problemas'),('solucao de problemas','problem-solving','Solução de problemas'),
 ('organizacao do tempo','time-management','Organização do tempo'),('gestao do tempo','time-management','Gestão do tempo'),
 ('atendimento ao cliente','customer-service','Atendimento ao cliente'),('redacao profissional','professional-writing','Redação profissional');
CREATE TABLE candidate_skill_declarations (
 user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 skill_code VARCHAR(64) NOT NULL REFERENCES skill_concepts(code),
 level INTEGER NOT NULL CHECK(level BETWEEN 1 AND 4), updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,skill_code)
);
CREATE TABLE candidate_radar_preferences (
 user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
 city VARCHAR(180) NOT NULL DEFAULT '', modality VARCHAR(40) NOT NULL DEFAULT '', area VARCHAR(120) NOT NULL DEFAULT '',
 experience_confirmed BOOLEAN NOT NULL DEFAULT FALSE, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE radar_catalog_requirements (
 item_id BIGINT PRIMARY KEY REFERENCES catalog_items(id) ON DELETE CASCADE,
 revision INTEGER NOT NULL DEFAULT 1, definition JSONB NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE radar_scoring_policy (
 id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL DEFAULT 1, weights JSONB NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO radar_scoring_policy(id,weights) VALUES(1,'{"MANDATORY":50,"DESIRABLE":15,"EXPERIENCE":10,"EDUCATION":10,"LOCATION":5,"MODALITY":5,"PREFERENCES":5}');
CREATE TABLE radar_claim_challenges (
 token_hash VARCHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 expires_at TIMESTAMPTZ NOT NULL, used_receipt UUID, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_radar_claim_user ON radar_claim_challenges(user_id,created_at DESC);
CREATE INDEX idx_radar_claim_expiry ON radar_claim_challenges(expires_at);
CREATE TABLE candidate_skill_evidence (
 receipt_id UUID PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 skill_code VARCHAR(64) NOT NULL REFERENCES skill_concepts(code), level INTEGER NOT NULL CHECK(level BETWEEN 1 AND 4),
 origin VARCHAR(16) NOT NULL CHECK(origin='CAPACITA'), course_title VARCHAR(180) NOT NULL,
 criterion VARCHAR(2000) NOT NULL, awarded_at TIMESTAMPTZ NOT NULL, consent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_radar_evidence_user ON candidate_skill_evidence(user_id,skill_code);
CREATE TABLE radar_match_cache (
 user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 source VARCHAR(16) NOT NULL CHECK(source IN ('catalog','company')), job_id BIGINT NOT NULL,
 fingerprint VARCHAR(64) NOT NULL, result JSONB NOT NULL, evaluated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,source,job_id)
);
CREATE INDEX idx_radar_cache_cleanup ON radar_match_cache(evaluated_at);
CREATE TABLE radar_audit (
 id BIGSERIAL PRIMARY KEY, actor_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
 event VARCHAR(40) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_radar_catalog_job_cursor ON catalog_items(id DESC) WHERE kind='job' AND active=TRUE AND moderation_status='APPROVED';
CREATE INDEX idx_radar_user_cache_recent ON radar_match_cache(user_id,evaluated_at DESC);
