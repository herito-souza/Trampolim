CREATE TABLE learning_materials (
 id BIGSERIAL PRIMARY KEY,
 course_id BIGINT NOT NULL REFERENCES learning_courses(id) ON DELETE CASCADE,
 subject_id BIGINT REFERENCES learning_subjects(id) ON DELETE SET NULL,
 module_id BIGINT REFERENCES learning_modules(id) ON DELETE SET NULL,
 teacher_id BIGINT NOT NULL REFERENCES student_users(id),
 title VARCHAR(180) NOT NULL,
 description VARCHAR(4000) NOT NULL DEFAULT '',
 topic VARCHAR(180) NOT NULL DEFAULT '',
 category VARCHAR(100) NOT NULL DEFAULT '',
 alternative_text TEXT NOT NULL DEFAULT '',
 access_scope VARCHAR(20) NOT NULL DEFAULT 'STUDENTS' CHECK(access_scope IN ('STUDENTS','TEACHERS')),
 allow_download BOOLEAN NOT NULL DEFAULT TRUE,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','HIDDEN','DELETED')),
 current_version_id BIGINT,
 published_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted_at TIMESTAMPTZ,
 revision INTEGER NOT NULL DEFAULT 0,
 CHECK(status<>'PUBLISHED' OR current_version_id IS NOT NULL)
);
CREATE TABLE learning_material_versions (
 id BIGSERIAL PRIMARY KEY,
 material_id BIGINT NOT NULL REFERENCES learning_materials(id) ON DELETE CASCADE,
 version_number INTEGER NOT NULL,
 storage_key UUID NOT NULL UNIQUE,
 sha256 VARCHAR(64) NOT NULL,
 size_bytes BIGINT NOT NULL CHECK(size_bytes>0),
 page_count INTEGER NOT NULL CHECK(page_count>0),
 has_text BOOLEAN NOT NULL,
 tagged BOOLEAN NOT NULL,
 uploaded_by BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(material_id,version_number),
 UNIQUE(material_id,sha256),
 UNIQUE(material_id,id)
);
ALTER TABLE learning_materials ADD CONSTRAINT fk_material_current_version FOREIGN KEY(id,current_version_id) REFERENCES learning_material_versions(material_id,id) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE learning_material_lessons (
 material_id BIGINT NOT NULL REFERENCES learning_materials(id) ON DELETE CASCADE,
 lesson_id BIGINT NOT NULL REFERENCES learning_lessons(id) ON DELETE CASCADE,
 PRIMARY KEY(material_id,lesson_id)
);
CREATE INDEX idx_material_lessons_reverse ON learning_material_lessons(lesson_id,material_id);
CREATE INDEX idx_material_course_status ON learning_materials(course_id,status,updated_at);
CREATE INDEX idx_material_teacher ON learning_materials(teacher_id,status);
CREATE TABLE learning_library_uploads (
 user_id BIGINT PRIMARY KEY REFERENCES student_users(id) ON DELETE CASCADE,
 attempts INTEGER NOT NULL DEFAULT 0,
 window_started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE learning_library_audit (
 id BIGSERIAL PRIMARY KEY,
 actor_id BIGINT REFERENCES student_users(id) ON DELETE SET NULL,
 material_id BIGINT,
 event VARCHAR(40) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
