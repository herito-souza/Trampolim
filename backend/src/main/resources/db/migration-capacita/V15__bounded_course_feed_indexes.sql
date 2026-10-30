-- Covers the published feed ordering; existing teacher/status index remains useful for ownership.
CREATE INDEX idx_learning_courses_public_feed ON learning_courses(updated_at DESC,id DESC)
    WHERE status='PUBLISHED' AND moderation_blocked=FALSE;
CREATE INDEX idx_learning_courses_teacher_feed ON learning_courses(teacher_id,updated_at DESC,id DESC);
