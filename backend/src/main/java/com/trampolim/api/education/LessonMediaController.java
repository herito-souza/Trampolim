package com.trampolim.api.education;

import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/capacita/learning/lessons")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LessonMediaController {
    private final JdbcTemplate jdbc;private final LearningAccess access;private final LearningController learning;private final LessonMediaStore store;private final TransactionTemplate transactions;
    private final java.util.concurrent.ConcurrentHashMap<Long,java.time.Instant> uploads=new java.util.concurrent.ConcurrentHashMap<>();
    public LessonMediaController(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc,LearningAccess access,LearningController learning,LessonMediaStore store,@Qualifier("capacitaTransactionManager") PlatformTransactionManager manager) {this.jdbc=jdbc;this.access=access;this.learning=learning;this.store=store;this.transactions=new TransactionTemplate(manager);}
    @PostMapping(value="/{id}/video",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam("file") MultipartFile file) {
        var user=access.teacher(email);access.ownLesson(user,id);
        var now=java.time.Instant.now();uploads.entrySet().removeIf(entry->entry.getValue().plusSeconds(300).isBefore(now));
        if(uploads.putIfAbsent(user.id(),now)!=null) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde a validação do vídeo anterior.");
        LessonMediaStore.Stored stored=null;boolean reserved=false,committed=false;
        try {
            reserveUpload(user.id());reserved=true;
            stored=store.store(file);var ready=stored;
            UUID previous=transactions.execute(status->{
                access.ownLesson(access.teacher(email),id);
                Long owner=jdbc.queryForObject("SELECT c.teacher_id FROM learning_lessons l JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id JOIN learning_courses c ON c.id=s.course_id WHERE l.id=?",Long.class,id);
                jdbc.queryForObject("SELECT id FROM student_users WHERE id=? FOR UPDATE",Long.class,owner);
                Long occupied=jdbc.queryForObject("SELECT COALESCE(SUM(l.media_bytes),0) FROM learning_lessons l JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id JOIN learning_courses c ON c.id=s.course_id WHERE c.teacher_id=? AND l.id<>?",Long.class,owner,id);
                if(occupied+ready.bytes()>536870912L) throw new ResponseStatusException(HttpStatus.CONFLICT,"Este vídeo excede a cota de 512 MB do professor responsável.");
                var old=jdbc.query("SELECT media_key FROM learning_lessons WHERE id=? FOR UPDATE",(rs,row)->(UUID)rs.getObject(1),id);
                jdbc.update("UPDATE learning_lessons SET media_key=?,media_type='video/mp4',media_bytes=?,duration_seconds=?,validation_status='READY',status='DRAFT',updated_at=CURRENT_TIMESTAMP WHERE id=?",ready.key(),ready.bytes(),ready.duration(),id);
                jdbc.update("DELETE FROM learning_lesson_progress WHERE lesson_id=?",id);
                jdbc.update("DELETE FROM learning_playback_sessions WHERE lesson_id=?",id);
                return old.isEmpty()?null:old.get(0);
            });
            committed=true;
            if(previous!=null) store.delete(previous);
            return Map.of("durationSeconds",stored.duration(),"validationStatus","READY","status","DRAFT");
        } catch(RuntimeException error) {if(stored!=null&&!committed) store.delete(stored.key());throw error;}
        finally {uploads.remove(user.id());if(reserved) jdbc.update("UPDATE learning_upload_limits SET active_until=NULL WHERE user_id=?",user.id());}
    }
    private void reserveUpload(long userId) {
        transactions.executeWithoutResult(status->{
            jdbc.update("INSERT INTO learning_upload_limits(user_id) VALUES (?) ON CONFLICT DO NOTHING",userId);
            var row=jdbc.queryForMap("SELECT attempts,window_started_at,active_until FROM learning_upload_limits WHERE user_id=? FOR UPDATE",userId);
            var now=java.time.Instant.now();var active=(java.sql.Timestamp)row.get("active_until");
            if(active!=null&&active.toInstant().isAfter(now)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Há um vídeo em processamento nesta conta.");
            boolean reset=((java.sql.Timestamp)row.get("window_started_at")).toInstant().isBefore(now.minusSeconds(3600));
            int count=reset?0:((Number)row.get("attempts")).intValue();
            if(count>=12) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite de 12 envios por hora atingido. Aguarde antes de tentar novamente.");
            Long usage=jdbc.queryForObject("SELECT COALESCE(SUM(l.media_bytes),0) FROM learning_lessons l JOIN learning_modules m ON m.id=l.module_id JOIN learning_subjects s ON s.id=m.subject_id JOIN learning_courses c ON c.id=s.course_id WHERE c.teacher_id=?",Long.class,userId);
            if(usage!=null&&usage>=536870912L) throw new ResponseStatusException(HttpStatus.CONFLICT,"O armazenamento de vídeos desta conta atingiu 512 MB. Procure a administração.");
            jdbc.update("UPDATE learning_upload_limits SET attempts=?,window_started_at=?,active_until=CURRENT_TIMESTAMP+INTERVAL '5 minutes' WHERE user_id=?",count+1,reset?java.sql.Timestamp.from(now):row.get("window_started_at"),userId);
        });
    }
    @GetMapping("/{id}/video") public ResponseEntity<Resource> video(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="false") boolean manage) {
        var user=access.account(email);learning.visibleLesson(user,id,manage);
        var keys=jdbc.query("SELECT media_key FROM learning_lessons WHERE id=? AND validation_status='READY'",(rs,row)->(UUID)rs.getObject(1),id);
        if(keys.isEmpty()||keys.get(0)==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var resource=new FileSystemResource(store.file(keys.get(0)));
        if(!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Vídeo indisponível.");
        return ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4")).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(resource);
    }
    @GetMapping(value="/{id}/captions",produces="text/vtt") public ResponseEntity<String> captions(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="false") boolean manage) {
        learning.visibleLesson(access.account(email),id,manage);
        String vtt=jdbc.queryForObject("SELECT captions_vtt FROM learning_lessons WHERE id=?",String.class,id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(vtt==null?"WEBVTT\n":vtt);
    }
}
