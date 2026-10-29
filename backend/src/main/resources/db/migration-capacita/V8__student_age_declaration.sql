-- Existing accounts retain NULL: no age is inferred from education stage or legacy data.
ALTER TABLE student_users ADD COLUMN age_band VARCHAR(16) CHECK (age_band IN ('AGE_14_17','AGE_18_PLUS'));
ALTER TABLE student_users ADD COLUMN age_declared_at TIMESTAMPTZ;
ALTER TABLE student_users ADD CONSTRAINT ck_student_age_declaration_pair CHECK ((age_band IS NULL) = (age_declared_at IS NULL));
