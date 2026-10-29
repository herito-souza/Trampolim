CREATE TABLE education_tracks (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE education_categories (
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES education_tracks(id) ON DELETE CASCADE,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(600) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    UNIQUE(track_id, code)
);

CREATE TABLE education_contents (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES education_categories(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(3000) NOT NULL,
    difficulty VARCHAR(40),
    resource_type VARCHAR(40) NOT NULL DEFAULT 'Material',
    source_url VARCHAR(1000),
    is_free BOOLEAN NOT NULL DEFAULT FALSE,
    has_certificate BOOLEAN NOT NULL DEFAULT FALSE,
    duration_minutes INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_capacita_contents_category ON education_contents(category_id, status, created_at DESC);

CREATE TABLE student_education_progress (
    student_user_id BIGINT NOT NULL REFERENCES student_users(id) ON DELETE CASCADE,
    content_id BIGINT NOT NULL REFERENCES education_contents(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(student_user_id, content_id)
);

INSERT INTO education_tracks(code, title, description, sort_order) VALUES
('HIGH_SCHOOL', 'Ensino médio', 'Apoio às disciplinas escolares, à organização da rotina e à preparação para avaliações.', 1),
('UNIVERSITY', 'Ensino superior', 'Fundamentos e práticas para a adaptação à vida acadêmica e o desenvolvimento profissional.', 2),
('CAREER_START', 'Conclusão do ensino médio e início profissional', 'Preparação para a transição da escola para o primeiro emprego e para oportunidades de estágio.', 3)
ON CONFLICT (code) DO NOTHING;

INSERT INTO education_categories(track_id, code, name, description, sort_order)
SELECT t.id, c.code, c.name, c.description, c.sort_order
FROM education_tracks t
JOIN (VALUES
 ('HIGH_SCHOOL','math','Matemática','Conteúdos de matemática escolar e raciocínio lógico.',1),
 ('HIGH_SCHOOL','portuguese','Língua Portuguesa e interpretação','Leitura, interpretação e produção de textos.',2),
 ('HIGH_SCHOOL','natural_sciences','Ciências da Natureza','Biologia, física e química para apoiar os estudos.',3),
 ('HIGH_SCHOOL','humanities','História e Geografia','Sociedade, território, história e atualidades.',4),
 ('HIGH_SCHOOL','study_skills','Técnicas de estudo e organização','Planejamento de rotina, foco e hábitos de aprendizagem.',5),
 ('HIGH_SCHOOL','assessments','Avaliações e vestibulares','Estratégias de preparação e revisão de conteúdos.',6),
 ('UNIVERSITY','academic_basics','Fundamentos acadêmicos','Adaptação à rotina, ferramentas e práticas do ensino superior.',1),
 ('UNIVERSITY','research','Metodologia científica','Pesquisa, fontes, referências e construção de projetos.',2),
 ('UNIVERSITY','academic_writing','Leitura e produção acadêmica','Compreensão e escrita de textos acadêmicos.',3),
 ('UNIVERSITY','study_planning','Organização dos estudos','Planejamento de tarefas e gestão do tempo.',4),
 ('UNIVERSITY','logic','Matemática e raciocínio lógico','Fundamentos quantitativos para diferentes cursos.',5),
 ('UNIVERSITY','professional_skills','Habilidades acadêmicas e profissionais','Comunicação, colaboração e competências para o trabalho.',6),
 ('CAREER_START','career_courses','Cursos de qualificação','Formação profissional e conhecimentos introdutórios.',1),
 ('CAREER_START','digital_tools','Informática e ferramentas digitais','Competências digitais úteis no estudo e no trabalho.',2),
 ('CAREER_START','communication','Comunicação e trabalho em equipe','Colaboração, comunicação e convivência profissional.',3),
 ('CAREER_START','interview_resume','Currículo e entrevista','Preparação para processos seletivos e apresentação profissional.',4),
 ('CAREER_START','market_skills','Competências profissionais','Organização, responsabilidade e aprendizagem contínua.',5),
 ('CAREER_START','internships','Vagas de estágio','Oportunidades de estágio divulgadas por empresas parceiras.',6)
) AS c(track_code, code, name, description, sort_order) ON t.code = c.track_code
ON CONFLICT (track_id, code) DO NOTHING;
