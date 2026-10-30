package com.trampolim.api.education;
import java.util.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/capacita/learning/discussion")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class DiscussionMediaController {
    private final JdbcTemplate jdbc;private final LearningAccess access;private final QuestionService questions;private final DiscussionMediaStore store;private final TransactionTemplate tx;private final com.trampolim.api.security.TrailTokenService tokens;
    @Value("${capacita.questions.media-uploads-hour:12}")private int uploadsHour=12;
    @Value("${capacita.questions.media-quota-bytes:268435456}")private long quota=268435456;
    @Value("${capacita.questions.closed-retention-days:0}")private int retentionDays;
    public DiscussionMediaController(@Qualifier("capacitaJdbcTemplate")JdbcTemplate jdbc,LearningAccess access,QuestionService questions,DiscussionMediaStore store,@Qualifier("capacitaTransactionManager")PlatformTransactionManager manager,com.trampolim.api.security.TrailTokenService tokens){this.jdbc=jdbc;this.access=access;this.questions=questions;this.store=store;this.tx=new TransactionTemplate(manager);this.tokens=tokens;}
    @GetMapping("/config")public Map<String,Object> config(@AuthenticationPrincipal String email){access.account(email);return store.limits();}
    @PostMapping(value="/media",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(@AuthenticationPrincipal String email,@RequestParam String kind,@RequestParam MultipartFile file) {
        var user=access.account(email);reserve(user.id());DiscussionMediaStore.Stored stored=null;
        try {
            stored=store.store(file,kind);var ready=stored;UUID id=UUID.randomUUID();
            tx.executeWithoutResult(status->{jdbc.queryForObject("SELECT id FROM student_users WHERE id=? FOR UPDATE",Long.class,user.id());if(jdbc.queryForObject("SELECT COALESCE(SUM(size_bytes),0) FROM learning_question_media WHERE owner_id=?",Long.class,user.id())+ready.bytes()>quota)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cota de mídias de dúvidas atingida.");jdbc.update("INSERT INTO learning_question_media(id,owner_id,storage_key,kind,media_type,duration_seconds,size_bytes) VALUES (?,?,?,?,?,?,?)",id,user.id(),ready.key(),ready.kind(),ready.mediaType(),ready.duration(),ready.bytes());});
            return Map.of("id",id,"kind",stored.kind(),"durationSeconds",stored.duration(),"sizeBytes",stored.bytes());
        }catch(RuntimeException error){if(stored!=null)store.delete(stored.key(),stored.kind());throw error;}
        finally{jdbc.update("UPDATE learning_communication_uploads SET active_until=NULL WHERE user_id=?",user.id());}
    }
    @PostMapping("/media/{id}/playback")public Map<String,Object> playback(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestHeader("Authorization")String authorization){
        var decoded=tokens.verify(authorization.replaceFirst("^Bearer ",""),com.trampolim.api.security.TrailTokenService.Trail.CAPACITA);
        if(decoded==null||email==null||!email.equalsIgnoreCase(decoded.getSubject()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user=access.account(email);questions.mediaAccess(user,id);if(jdbc.queryForObject("SELECT count(*) FROM learning_question_media_tickets WHERE user_id=? AND expires_at>CURRENT_TIMESTAMP",Integer.class,user.id())>=100)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        UUID ticket=UUID.randomUUID();int version=decoded.getClaim("ver").asInt();
        int created=jdbc.update("INSERT INTO learning_question_media_tickets(id,media_id,user_id,token_version,expires_at) SELECT ?,?,?,?,LEAST(CURRENT_TIMESTAMP+INTERVAL '10 minutes',?) FROM student_users WHERE id=? AND token_version=? AND account_status='ACTIVE' AND email_verified=TRUE",ticket,id,user.id(),version,java.sql.Timestamp.from(decoded.getExpiresAt().toInstant()),user.id(),version);
        if(created==0)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sua sessão foi encerrada.");return Map.of("path","/capacita/learning/discussion/media/play/"+ticket);
    }
    @GetMapping("/media/play/{ticket}")public ResponseEntity<Resource> read(@PathVariable UUID ticket) {
        var rows=jdbc.queryForList("SELECT t.media_id,u.email,a.storage_key,a.kind,a.media_type FROM learning_question_media_tickets t JOIN student_users u ON u.id=t.user_id JOIN learning_question_media a ON a.id=t.media_id WHERE t.id=? AND t.expires_at>CURRENT_TIMESTAMP AND t.token_version=u.token_version AND u.account_status='ACTIVE' AND u.email_verified=TRUE",ticket);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Mídia indisponível.");var row=rows.get(0);questions.mediaAccess(access.account((String)row.get("email")),(UUID)row.get("media_id"));
        var resource=new FileSystemResource(store.file((UUID)row.get("storage_key"),(String)row.get("kind")));if(!resource.exists())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.valueOf((String)row.get("media_type"))).header("X-Content-Type-Options","nosniff").body(resource);
    }
    @Scheduled(fixedDelayString="${capacita.questions.cleanup-ms:3600000}")public void cleanup() {
        try {
            jdbc.update("DELETE FROM learning_question_media_tickets WHERE expires_at<CURRENT_TIMESTAMP");
            if(retentionDays>0)jdbc.update("DELETE FROM learning_questions WHERE status='CLOSED' AND closed_at<CURRENT_TIMESTAMP-(? * INTERVAL '1 day')",retentionDays);
            var unused=jdbc.queryForList("SELECT a.id,a.storage_key,a.kind FROM learning_question_media a WHERE COALESCE(a.last_attached_at,a.created_at)<CURRENT_TIMESTAMP-INTERVAL '1 day' AND NOT EXISTS(SELECT 1 FROM learning_question_messages m WHERE m.media_id=a.id) LIMIT 100");
            for(var row:unused){int removed=jdbc.update("DELETE FROM learning_question_media a WHERE id=? AND COALESCE(a.last_attached_at,a.created_at)<CURRENT_TIMESTAMP-INTERVAL '1 day' AND NOT EXISTS(SELECT 1 FROM learning_question_messages m WHERE m.media_id=a.id)",row.get("id"));if(removed>0)store.delete((UUID)row.get("storage_key"),(String)row.get("kind"));}
            if(Files.isDirectory(store.directory()))try(var files=Files.list(store.directory())){for(Path path:files.limit(1000).toList()){
                String name=path.getFileName().toString();if(!name.matches("[0-9a-f-]{36}\\.(mp4|m4a)")||Files.getLastModifiedTime(path).toInstant().plusSeconds(86400).isAfter(java.time.Instant.now()))continue;
                UUID key=UUID.fromString(name.substring(0,36));if(jdbc.queryForObject("SELECT count(*) FROM learning_question_media WHERE storage_key=?",Integer.class,key)==0)Files.deleteIfExists(path);
            }}
        }catch(org.springframework.dao.DataAccessException|java.io.IOException ignored){/* Retry later; private files are never exposed directly. */}
    }
    private void reserve(long id){tx.executeWithoutResult(status->{jdbc.update("INSERT INTO learning_communication_uploads(user_id) VALUES (?) ON CONFLICT DO NOTHING",id);var row=jdbc.queryForMap("SELECT attempts,window_started_at,active_until FROM learning_communication_uploads WHERE user_id=? FOR UPDATE",id);var now=java.time.Instant.now();var active=(java.sql.Timestamp)row.get("active_until");if(active!=null&&active.toInstant().isAfter(now))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde o envio anterior.");boolean reset=((java.sql.Timestamp)row.get("window_started_at")).toInstant().plusSeconds(3600).isBefore(now);int count=reset?0:((Number)row.get("attempts")).intValue();if(count>=uploadsHour)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite de envios de mídia por hora atingido.");jdbc.update("UPDATE learning_communication_uploads SET attempts=?,window_started_at=?,active_until=CURRENT_TIMESTAMP+INTERVAL '4 minutes' WHERE user_id=?",count+1,reset?java.sql.Timestamp.from(now):row.get("window_started_at"),id);});}
}
