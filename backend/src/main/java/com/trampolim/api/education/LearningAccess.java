package com.trampolim.api.education;

import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LearningAccess {
    private final JdbcTemplate jdbc;
    @Value("${capacita.admin-emails:}") private String bootstrapAdmins;
    public LearningAccess(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public record Account(long id,String name,String email,boolean teacher,boolean admin) {}
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
    public Account teacher(String email) {var user=account(email);if(!user.teacher()) forbidden();return user;}
    public Account admin(String email) {var user=account(email);if(!user.admin()) forbidden();return user;}
    public void ownCourse(Account user,long id) {
        Long owner=jdbc.query("SELECT teacher_id FROM learning_courses WHERE id=?",(rs,row)->rs.getLong(1),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!user.admin()&&owner!=user.id()) forbidden();
    }
    public void ownModule(Account user,long id) {ownCourse(user,courseForModule(id));}
    public long courseForModule(long id) {return jdbc.query("SELECT s.course_id FROM learning_modules m JOIN learning_subjects s ON s.id=m.subject_id WHERE m.id=?",(rs,row)->rs.getLong(1),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    public long courseForLesson(long id) {return jdbc.query("SELECT s.course_id FROM learning_lessons l JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id WHERE l.id=?",(rs,row)->rs.getLong(1),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    public void ownLesson(Account user,long id) {ownCourse(user,courseForLesson(id));}
    private void forbidden() {throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Você não tem permissão para gerenciar este conteúdo.");}
}
