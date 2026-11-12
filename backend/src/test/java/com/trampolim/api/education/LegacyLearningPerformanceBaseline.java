package com.trampolim.api.education;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
/** Test-only reference copied from the audited pre-optimization implementation; never registered as a bean. */
final class LegacyLearningPerformanceBaseline {
 private final JdbcTemplate jdbc;private final LegacyAccess access;
 LegacyLearningPerformanceBaseline(JdbcTemplate jdbc){this.jdbc=jdbc;this.access=new LegacyAccess(jdbc);}
    public Map<String,Object> dashboard(String email){
        long student=access.account(email).id();var courses=jdbc.queryForList("SELECT c.id,c.title FROM learning_courses c JOIN student_users u ON u.id=c.teacher_id WHERE c.status='PUBLISHED' AND c.moderation_blocked=FALSE AND u.account_status='ACTIVE' AND (EXISTS(SELECT 1 FROM learning_lesson_progress p JOIN learning_lessons l ON l.id=p.lesson_id JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id WHERE p.student_id=? AND s.course_id=c.id) OR EXISTS(SELECT 1 FROM learning_assessment_attempts t JOIN learning_assessments a ON a.id=t.assessment_id WHERE t.student_id=? AND a.course_id=c.id)) ORDER BY c.id DESC LIMIT 200",student,student);
        long completedCourses=0,completedLessons=0,completedModules=0;
        // Queries operate only on the student's started courses. No classmate results are included.
        if(!courses.isEmpty()){
            String placeholders=String.join(",",Collections.nCopies(courses.size(),"?"));var args=new ArrayList<Object>();args.add(student);courses.forEach(course->args.add(course.get("id")));
            var lessons=jdbc.queryForList("SELECT s.course_id,m.id AS module_id,l.id,p.completed_at FROM learning_lessons l JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id LEFT JOIN learning_lesson_progress p ON p.lesson_id=l.id AND p.student_id=? WHERE l.status='PUBLISHED' AND l.validation_status='READY' AND s.course_id IN ("+placeholders+")",args.toArray());
            var required=jdbc.queryForList("SELECT a.id,a.course_id,a.module_id,EXISTS(SELECT 1 FROM learning_assessment_attempts t WHERE t.assessment_id=a.id AND t.student_id=? AND t.passed=TRUE) AS passed FROM learning_assessments a JOIN learning_assessment_versions v ON v.id=a.current_version_id WHERE a.status='PUBLISHED' AND (v.definition->>'requiredForCompletion')::boolean=TRUE AND a.course_id IN ("+placeholders+")",args.toArray());
            for(var course:courses){long id=((Number)course.get("id")).longValue();var rows=lessons.stream().filter(row->((Number)row.get("course_id")).longValue()==id).toList();long done=rows.stream().filter(row->row.get("completed_at")!=null).count();var tests=required.stream().filter(row->((Number)row.get("course_id")).longValue()==id).toList();boolean passed=tests.stream().allMatch(row->Boolean.TRUE.equals(row.get("passed")));var modules=new HashSet<Long>();rows.forEach(row->modules.add(((Number)row.get("module_id")).longValue()));long modulesDone=modules.stream().filter(module->rows.stream().filter(row->((Number)row.get("module_id")).longValue()==module).allMatch(row->row.get("completed_at")!=null)&&tests.stream().filter(row->row.get("module_id")!=null&&((Number)row.get("module_id")).longValue()==module).allMatch(row->Boolean.TRUE.equals(row.get("passed")))).count();boolean complete=!rows.isEmpty()&&done==rows.size()&&passed;if(complete)completedCourses++;completedLessons+=done;completedModules+=modulesDone;course.put("lessonsTotal",rows.size());course.put("lessonsCompleted",done);course.put("modulesTotal",modules.size());course.put("modulesCompleted",modulesDone);course.put("requiredAssessments",tests.size());course.put("passedRequiredAssessments",tests.stream().filter(row->Boolean.TRUE.equals(row.get("passed"))).count());course.put("progressPercent",rows.isEmpty()?0:Math.round(100d*done/rows.size()));course.put("completed",complete);}
        }
        long submitted=jdbc.queryForObject("SELECT count(*) FROM learning_assessment_attempts WHERE student_id=? AND status='SUBMITTED'",Long.class,student);long passed=jdbc.queryForObject("SELECT count(*) FROM learning_assessment_attempts WHERE student_id=? AND passed=TRUE",Long.class,student);long awards=jdbc.queryForObject("SELECT count(*) FROM learning_competency_awards WHERE student_id=?",Long.class,student);
        return Map.of("courses",courses,"coursesStarted",courses.size(),"coursesCompleted",completedCourses,"lessonsCompleted",completedLessons,"modulesCompleted",completedModules,"assessmentsSubmitted",submitted,"assessmentsPassed",passed,"competenciesEarned",awards);
    }
 private static final class LegacyAccess {
 private final JdbcTemplate jdbc;private final String bootstrapAdmins="";
 LegacyAccess(JdbcTemplate jdbc){this.jdbc=jdbc;}
 record Account(long id,String name,String email,boolean teacher,boolean admin){}
    public Account account(String email) {
        if(email==null||email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Entre no Capacita.");
        var rows=jdbc.query("SELECT id,full_name,email FROM student_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE' AND email_verified=TRUE",
            (rs,row)->new Object[]{rs.getLong(1),rs.getString(2),rs.getString(3)},email);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Conta educacional indisponível.");
        var row=rows.get(0); long id=(Long)row[0];
        var roles=jdbc.queryForList("SELECT role FROM learning_account_roles WHERE user_id=?",String.class,id);
        boolean admin=roles.contains("ADMIN")||Arrays.stream(bootstrapAdmins.split(",")).anyMatch(value->!value.isBlank()&&value.trim().equalsIgnoreCase(email));
        return new Account(id,(String)row[1],(String)row[2],admin||roles.contains("TEACHER"),admin);
    }
 }
}
