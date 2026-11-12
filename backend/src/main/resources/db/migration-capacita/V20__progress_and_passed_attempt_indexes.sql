SET LOCAL lock_timeout='5s';
CREATE INDEX idx_learning_progress_recent ON learning_lesson_progress(student_id,updated_at DESC,lesson_id);
CREATE INDEX idx_learning_attempt_passed ON learning_assessment_attempts(student_id,assessment_id) WHERE passed=TRUE;
