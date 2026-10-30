package com.trampolim.api.education;

import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/capacita/learning")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LearningController {
    private final JdbcTemplate jdbc; private final LearningAccess access;
    public LearningController(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc,LearningAccess access) {this.jdbc=jdbc;this.access=access;}
    public record CourseInput(String title,String description,String category,String imageUrl,String status) {}
    public record SectionInput(String title,String description,int sortOrder) {}
    public record LessonInput(String title,String description,String objectives,String transcript,String captionsVtt,int sortOrder,String status) {}
    public record RoleInput(String email,String role,boolean granted) {}
    public record StatusInput(String status) {}
    @GetMapping("/me") public LearningAccess.Account me(@AuthenticationPrincipal String email) {return access.account(email);}
    public List<Map<String,Object>> courses(String email,boolean manage) {return courses(email,manage,200,0,null);}
    @GetMapping("/courses") public List<Map<String,Object>> courses(@AuthenticationPrincipal String email,@RequestParam(defaultValue="false") boolean manage,
            @RequestParam(defaultValue="200") int limit,@RequestParam(defaultValue="0") int offset,@RequestParam(required=false) String q) {
        var user=access.account(email);
        if(limit<1||limit>200||offset<0||offset>10000||q!=null&&q.length()>120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Limites de consulta inválidos.");
        if(manage&&!user.teacher()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var parameters=new ArrayList<Object>();
        String where=manage?(user.admin()?" WHERE TRUE":" WHERE c.teacher_id=?"):" WHERE c.status='PUBLISHED' AND c.moderation_blocked=FALSE AND u.account_status='ACTIVE'";
        if(manage&&!user.admin()) parameters.add(user.id());
        if(q!=null&&!q.isBlank()) {where+=" AND (c.title ILIKE ? OR c.category ILIKE ?)";parameters.add("%"+q.trim()+"%");parameters.add("%"+q.trim()+"%");}
        parameters.add(limit);parameters.add(offset);
        String sql="SELECT c.*,u.full_name AS teacher_name,(SELECT count(*) FROM learning_subjects s JOIN learning_modules m ON m.subject_id=s.id JOIN learning_lessons l ON l.module_id=m.id WHERE s.course_id=c.id AND l.status='PUBLISHED') AS lessons_count FROM learning_courses c JOIN student_users u ON u.id=c.teacher_id"+where+" ORDER BY c.updated_at DESC,c.id DESC LIMIT ? OFFSET ?";
        return jdbc.queryForList(sql,parameters.toArray());
    }
    @PostMapping("/courses") public Map<String,Object> createCourse(@AuthenticationPrincipal String email,@RequestBody CourseInput input) {
        var user=access.teacher(email);validateCourse(input);
        Long id=jdbc.queryForObject("INSERT INTO learning_courses(teacher_id,title,description,category,image_url,status) VALUES (?,?,?,?,?,?) RETURNING id",Long.class,user.id(),input.title().trim(),text(input.description(),4000),input.category().trim(),image(input.imageUrl()),status(input.status()));
        return Map.of("id",id);
    }
    @PutMapping("/courses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void editCourse(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody CourseInput input) {
        var user=access.teacher(email);access.ownCourse(user,id);validateCourse(input);
        if(!user.admin()&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT moderation_blocked FROM learning_courses WHERE id=?",Boolean.class,id)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Este curso foi bloqueado pela moderação. Procure a administração educacional.");
        jdbc.update("UPDATE learning_courses SET title=?,description=?,category=?,image_url=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",input.title().trim(),text(input.description(),4000),input.category().trim(),image(input.imageUrl()),status(input.status()),id);
    }
    @GetMapping("/courses/{id}") public Map<String,Object> course(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="false") boolean manage) {
        var user=access.account(email);visibleCourse(user,id,manage);
        var course=jdbc.queryForMap("SELECT c.*,u.full_name AS teacher_name FROM learning_courses c JOIN student_users u ON u.id=c.teacher_id WHERE c.id=?",id);
        var subjects=jdbc.queryForList("SELECT * FROM learning_subjects WHERE course_id=? ORDER BY sort_order,id",id);
        var modules=jdbc.queryForList("SELECT m.* FROM learning_modules m JOIN learning_subjects s ON s.id=m.subject_id WHERE s.course_id=? ORDER BY m.sort_order,m.id",id);
        var lessons=jdbc.queryForList("SELECT l.id,l.module_id,l.title,l.description,l.objectives,l.sort_order,l.status,l.duration_seconds,l.validation_status FROM learning_lessons l JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id WHERE s.course_id=?"+(manage?"":" AND l.status='PUBLISHED' AND l.validation_status='READY'")+" ORDER BY l.sort_order,l.id",id);
        Map<Long,List<Map<String,Object>>> lessonsByModule=new HashMap<>();
        for(var lesson:lessons) lessonsByModule.computeIfAbsent(((Number)lesson.get("module_id")).longValue(),key->new ArrayList<>()).add(lesson);
        Map<Long,List<Map<String,Object>>> modulesBySubject=new HashMap<>();
        for(var module:modules) { module.put("lessons",lessonsByModule.getOrDefault(((Number)module.get("id")).longValue(),List.of())); modulesBySubject.computeIfAbsent(((Number)module.get("subject_id")).longValue(),key->new ArrayList<>()).add(module); }
        for(var subject:subjects) subject.put("modules",modulesBySubject.getOrDefault(((Number)subject.get("id")).longValue(),List.of()));
        course.put("subjects",subjects);return course;
    }
    @PostMapping("/courses/{id}/subjects") public Map<String,Object> createSubject(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody SectionInput input) {
        access.ownCourse(access.teacher(email),id);section(input);
        return Map.of("id",jdbc.queryForObject("INSERT INTO learning_subjects(course_id,title,description,sort_order) VALUES (?,?,?,?) RETURNING id",Long.class,id,input.title().trim(),text(input.description(),2000),input.sortOrder()));
    }
    @PutMapping("/subjects/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void editSubject(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody SectionInput input) {
        access.ownCourse(access.teacher(email),subjectCourse(id));section(input);
        jdbc.update("UPDATE learning_subjects SET title=?,description=?,sort_order=? WHERE id=?",input.title().trim(),text(input.description(),2000),input.sortOrder(),id);
    }
    @PostMapping("/subjects/{id}/modules") public Map<String,Object> createModule(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody SectionInput input) {
        access.ownCourse(access.teacher(email),subjectCourse(id));section(input);
        return Map.of("id",jdbc.queryForObject("INSERT INTO learning_modules(subject_id,title,description,sort_order) VALUES (?,?,?,?) RETURNING id",Long.class,id,input.title().trim(),text(input.description(),2000),input.sortOrder()));
    }
    @PutMapping("/modules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void editModule(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody SectionInput input) {
        access.ownModule(access.teacher(email),id);section(input);
        jdbc.update("UPDATE learning_modules SET title=?,description=?,sort_order=? WHERE id=?",input.title().trim(),text(input.description(),2000),input.sortOrder(),id);
    }
    @PostMapping("/modules/{id}/lessons") public Map<String,Object> createLesson(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody LessonInput input) {
        access.ownModule(access.teacher(email),id);lesson(input);
        if(!"DRAFT".equals(status(input.status()))) bad("Envie e valide o vídeo antes de publicar a aula.");
        return Map.of("id",jdbc.queryForObject("INSERT INTO learning_lessons(module_id,title,description,objectives,transcript,captions_vtt,sort_order) VALUES (?,?,?,?,?,?,?) RETURNING id",Long.class,id,input.title().trim(),text(input.description(),4000),text(input.objectives(),4000),text(input.transcript(),100000),captions(input.captionsVtt()),input.sortOrder()));
    }
    @PutMapping("/lessons/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void editLesson(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody LessonInput input) {
        access.ownLesson(access.teacher(email),id);lesson(input);String desired=status(input.status());
        if("PUBLISHED".equals(desired)&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT validation_status='READY' AND media_key IS NOT NULL AND duration_seconds BETWEEN 180 AND 300 FROM learning_lessons WHERE id=?",Boolean.class,id))) bad("A aula precisa de um vídeo validado de 180 a 300 segundos.");
        jdbc.update("UPDATE learning_lessons SET title=?,description=?,objectives=?,transcript=?,captions_vtt=?,sort_order=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",input.title().trim(),text(input.description(),4000),text(input.objectives(),4000),text(input.transcript(),100000),captions(input.captionsVtt()),input.sortOrder(),desired,id);
    }
    @GetMapping("/lessons/{id}") public Map<String,Object> lesson(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="false") boolean manage) {
        var user=access.account(email);visibleLesson(user,id,manage);
        var result=jdbc.queryForMap("SELECT id,module_id,title,description,objectives,transcript,captions_vtt,sort_order,status,duration_seconds,validation_status FROM learning_lessons WHERE id=?",id);
        result.put("progress",jdbc.queryForList("SELECT position_seconds,watched_seconds,completed_at FROM learning_lesson_progress WHERE student_id=? AND lesson_id=?",user.id(),id).stream().findFirst().orElse(Map.of()));return result;
    }
    @GetMapping("/progress") public List<Map<String,Object>> progress(@AuthenticationPrincipal String email) {
        return jdbc.queryForList("SELECT p.*,l.title,c.id AS course_id,c.title AS course_title FROM learning_lesson_progress p JOIN learning_lessons l ON l.id=p.lesson_id JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id JOIN learning_courses c ON c.id=s.course_id WHERE p.student_id=? AND c.status='PUBLISHED' AND l.status='PUBLISHED' ORDER BY p.updated_at DESC LIMIT 100",access.account(email).id());
    }
    @GetMapping("/courses/{id}/students") public List<Map<String,Object>> studentProgress(@AuthenticationPrincipal String email,@PathVariable long id) {
        access.ownCourse(access.teacher(email),id);
        return jdbc.queryForList("SELECT u.id,u.full_name,COUNT(*) AS started_lessons,COUNT(p.completed_at) AS completed_lessons,MAX(p.updated_at) AS last_activity FROM learning_lesson_progress p JOIN student_users u ON u.id=p.student_id JOIN learning_lessons l ON l.id=p.lesson_id JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id WHERE s.course_id=? GROUP BY u.id,u.full_name ORDER BY u.full_name LIMIT 200",id);
    }
    @GetMapping("/admin/accounts") public List<Map<String,Object>> accounts(@AuthenticationPrincipal String email) {
        access.admin(email);return jdbc.queryForList("SELECT u.id,u.full_name,u.email,COALESCE(string_agg(r.role,','),'STUDENT') AS roles FROM student_users u LEFT JOIN learning_account_roles r ON r.user_id=u.id GROUP BY u.id ORDER BY u.id DESC LIMIT 200");
    }
    @PutMapping("/admin/roles") @Transactional(transactionManager="capacitaTransactionManager")
    @ResponseStatus(HttpStatus.NO_CONTENT) public void role(@AuthenticationPrincipal String email,@RequestBody RoleInput input) {
        var admin=access.admin(email);
        if(input==null||input.email()==null||!Set.of("ADMIN","TEACHER").contains(input.role())) bad("Informe a conta e um papel válido.");
        Long target=jdbc.query("SELECT id FROM student_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE'",(rs,row)->rs.getLong(1),input.email()).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Conta do Capacita não encontrada."));
        if(target==admin.id()&&!input.granted()) bad("Outro administrador deve alterar seu papel.");
        if(input.granted()) jdbc.update("INSERT INTO learning_account_roles(user_id,role) VALUES (?,?) ON CONFLICT DO NOTHING",target,input.role());
        else jdbc.update("DELETE FROM learning_account_roles WHERE user_id=? AND role=?",target,input.role());
        jdbc.update("INSERT INTO learning_role_audit(actor_id,target_id,role,granted) VALUES (?,?,?,?)",admin.id(),target,input.role(),input.granted());
    }
    @PutMapping("/admin/courses/{id}/status") @ResponseStatus(HttpStatus.NO_CONTENT) public void moderate(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody StatusInput input) {
        access.admin(email);if(input==null) bad("Informe o status.");
        String desired=status(input.status());
        if(jdbc.update("UPDATE learning_courses SET status=?,moderation_blocked=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",desired,"HIDDEN".equals(desired),id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    public void visibleLesson(LearningAccess.Account user,long id,boolean manage) {
        long course=access.courseForLesson(id);visibleCourse(user,course,manage);
        if(!manage&&jdbc.queryForObject("SELECT count(*) FROM learning_lessons WHERE id=? AND status='PUBLISHED' AND validation_status='READY'",Integer.class,id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Aula indisponível.");
    }
    public void visibleCourse(LearningAccess.Account user,long id,boolean manage) {
        if(manage) {if(!user.teacher()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);access.ownCourse(user,id);return;}
        if(jdbc.queryForObject("SELECT count(*) FROM learning_courses c JOIN student_users u ON u.id=c.teacher_id WHERE c.id=? AND c.status='PUBLISHED' AND c.moderation_blocked=FALSE AND u.account_status='ACTIVE'",Integer.class,id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Curso indisponível.");
    }
    private long subjectCourse(long id) {return jdbc.query("SELECT course_id FROM learning_subjects WHERE id=?",(rs,row)->rs.getLong(1),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    private void validateCourse(CourseInput input) {if(input==null) bad("Informe o curso.");required(input.title(),180);required(input.category(),120);text(input.description(),4000);}
    private void section(SectionInput input) {if(input==null) bad("Informe os dados.");required(input.title(),180);if(input.sortOrder()<0) bad("Ordem inválida.");text(input.description(),2000);}
    private void lesson(LessonInput input) {if(input==null) bad("Informe a aula.");required(input.title(),180);if(input.sortOrder()<0) bad("Ordem inválida.");text(input.description(),4000);text(input.objectives(),4000);}
    private String status(String value) {String result=value==null?"DRAFT":value;if(!Set.of("DRAFT","PUBLISHED","HIDDEN").contains(result)) bad("Status inválido.");return result;}
    private String image(String value) {if(value==null||value.isBlank()) return null;if(!value.startsWith("https://")||value.length()>1000) bad("Use uma URL HTTPS para a imagem.");return value;}
    private void required(String value,int max) {if(value==null||value.isBlank()||value.length()>max) bad("Preencha os campos obrigatórios dentro do limite.");}
    private String text(String value,int max) {String result=value==null?"":value.trim();if(result.length()>max) bad("Texto acima do limite permitido.");return result;}
    private String captions(String value) {String result=text(value,100000);if(!result.isEmpty()&&(!result.startsWith("WEBVTT")||result.contains("<script"))) bad("Envie legendas no formato WEBVTT.");return result;}
    private void bad(String message) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
