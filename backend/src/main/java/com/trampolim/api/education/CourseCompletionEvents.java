package com.trampolim.api.education;

import com.trampolim.api.events.DomainEventOutbox;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Emits a single milestone only when the platform's existing completion criteria are satisfied. */
@Component
public class CourseCompletionEvents {
    private final DomainEventOutbox outbox;

    public CourseCompletionEvents(DomainEventOutbox outbox) { this.outbox = outbox; }

    public void emitIfComplete(JdbcTemplate jdbc, long studentId, long courseId) {
        var rows = jdbc.queryForList("""
                SELECT
                  (SELECT count(*) FROM learning_lessons l JOIN learning_modules m ON m.id=l.module_id
                     JOIN learning_subjects s ON s.id=m.subject_id
                   WHERE s.course_id=c.id AND l.status='PUBLISHED' AND l.validation_status='READY') AS lessons_total,
                  (SELECT count(*) FROM learning_lesson_progress p JOIN learning_lessons l ON l.id=p.lesson_id
                     JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id
                   WHERE s.course_id=c.id AND p.student_id=? AND p.completed_at IS NOT NULL
                     AND l.status='PUBLISHED' AND l.validation_status='READY') AS lessons_completed,
                  (SELECT count(*) FROM learning_assessments a JOIN learning_assessment_versions v ON v.id=a.current_version_id
                   WHERE a.course_id=c.id AND a.status='PUBLISHED'
                     AND COALESCE((v.definition->>'requiredForCompletion')::boolean,FALSE)=TRUE) AS required_assessments,
                  (SELECT count(*) FROM learning_assessments a JOIN learning_assessment_versions v ON v.id=a.current_version_id
                   WHERE a.course_id=c.id AND a.status='PUBLISHED'
                     AND COALESCE((v.definition->>'requiredForCompletion')::boolean,FALSE)=TRUE
                     AND NOT EXISTS(SELECT 1 FROM learning_assessment_attempts t WHERE t.assessment_id=a.id
                       AND t.student_id=? AND t.status='SUBMITTED' AND t.passed=TRUE)) AS pending_assessments
                FROM learning_courses c JOIN student_users teacher ON teacher.id=c.teacher_id
                WHERE c.id=? AND c.status='PUBLISHED' AND c.moderation_blocked=FALSE AND teacher.account_status='ACTIVE'
                """, studentId, studentId, courseId);
        if (rows.isEmpty()) return;
        var row = rows.getFirst();
        long total = ((Number) row.get("lessons_total")).longValue();
        long completed = ((Number) row.get("lessons_completed")).longValue();
        long required = ((Number) row.get("required_assessments")).longValue();
        long pending = ((Number) row.get("pending_assessments")).longValue();
        if (total == 0 || completed != total || pending != 0) return;
        outbox.append(jdbc, "CAPACITA", "course.completed.v1", "course_enrollment",
                studentId + ":" + courseId, "course-completed:" + studentId + ":" + courseId,
                Map.of("studentId", studentId, "courseId", courseId,
                        "completionCriteria", "ALL_READY_PUBLISHED_LESSONS_AND_REQUIRED_ASSESSMENTS_V1",
                        "lessonsTotal", total, "requiredAssessments", required));
    }
}
