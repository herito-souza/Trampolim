package com.trampolim.api.education;

import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/capacita/learning")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LearningPlaybackController {
    private final JdbcTemplate jdbc;private final LearningAccess access;private final LearningController learning;private final LessonMediaController media;private final com.trampolim.api.security.TrailTokenService tokens;
    public LearningPlaybackController(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc,LearningAccess access,LearningController learning,LessonMediaController media,com.trampolim.api.security.TrailTokenService tokens) {this.jdbc=jdbc;this.access=access;this.learning=learning;this.media=media;this.tokens=tokens;}
    public record PlaybackInput(UUID sessionId,double positionSeconds,boolean playing) {}
    @PostMapping("/lessons/{id}/playback") @Transactional(transactionManager="capacitaTransactionManager")
    public Map<String,Object> start(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="false") boolean manage,@RequestHeader("Authorization")String authorization) {
        var user=access.account(email);learning.visibleLesson(user,id,manage);
        var decoded=tokens.verify(authorization.replaceFirst("^Bearer ",""),com.trampolim.api.security.TrailTokenService.Trail.CAPACITA);
        if(decoded==null||!email.equalsIgnoreCase(decoded.getSubject())||!Objects.equals(user.id(),decoded.getClaim("uid").asLong()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT validation_status='READY' FROM learning_lessons WHERE id=?",Boolean.class,id))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Envie e valide o vídeo antes de assistir.");
        jdbc.update("DELETE FROM learning_playback_sessions WHERE expires_at<CURRENT_TIMESTAMP");
        if(jdbc.queryForObject("SELECT count(*) FROM learning_playback_sessions WHERE student_id=? AND last_event_at>CURRENT_TIMESTAMP-INTERVAL '1 minute'",Integer.class,user.id())>=12) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde antes de abrir outra reprodução.");
        UUID session=UUID.randomUUID();
        Double position=jdbc.query("SELECT position_seconds FROM learning_lesson_progress WHERE student_id=? AND lesson_id=?",(rs,row)->rs.getDouble(1),user.id(),id).stream().findFirst().orElse(0d);
        int created=jdbc.update("INSERT INTO learning_playback_sessions(id,student_id,lesson_id,last_position,preview,token_version,expires_at) SELECT ?,?,?,?,?,?,LEAST(CURRENT_TIMESTAMP+INTERVAL '30 minutes',?) FROM student_users WHERE id=? AND token_version=? AND account_status='ACTIVE' AND email_verified=TRUE",session,user.id(),id,position,manage,decoded.getClaim("ver").asInt(),java.sql.Timestamp.from(decoded.getExpiresAtAsInstant()),user.id(),decoded.getClaim("ver").asInt());
        if(created==0)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"A sessão foi encerrada. Entre novamente para assistir.");
        return Map.of("sessionId",session,"positionSeconds",position,"videoPath","/capacita/learning/media/"+session,"captionsPath","/capacita/learning/media/"+session+"/captions");
    }
    @GetMapping("/media/{ticket}") public ResponseEntity<Resource> video(@PathVariable UUID ticket) {var session=ticket(ticket);return media.video((String)session.get("email"),((Number)session.get("lesson_id")).longValue(),(Boolean)session.get("preview"));}
    @GetMapping(value="/media/{ticket}/captions",produces="text/vtt") public ResponseEntity<String> captions(@PathVariable UUID ticket) {var session=ticket(ticket);return media.captions((String)session.get("email"),((Number)session.get("lesson_id")).longValue(),(Boolean)session.get("preview"));}
    @PostMapping("/lessons/{id}/progress") @Transactional(transactionManager="capacitaTransactionManager")
    public Map<String,Object> update(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody PlaybackInput input) {
        var user=access.account(email);learning.visibleLesson(user,id,false);
        if(input==null||input.sessionId()==null||!Double.isFinite(input.positionSeconds())||input.positionSeconds()<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Progresso inválido.");
        var rows=jdbc.queryForList("SELECT s.last_position,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP-s.last_event_at) AS elapsed FROM learning_playback_sessions s JOIN student_users u ON u.id=s.student_id WHERE s.id=? AND s.student_id=? AND s.lesson_id=? AND s.preview=FALSE AND s.expires_at>CURRENT_TIMESTAMP AND s.token_version=u.token_version AND u.account_status='ACTIVE' FOR UPDATE OF s",input.sessionId(),user.id(),id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Reabra a aula para renovar a reprodução.");
        var row=rows.get(0);double duration=jdbc.queryForObject("SELECT duration_seconds FROM learning_lessons WHERE id=?",Double.class,id);
        double position=Math.min(duration,input.positionSeconds());double last=((Number)row.get("last_position")).doubleValue();double elapsed=((Number)row.get("elapsed")).doubleValue();
        if(elapsed<1) return progress(user.id(),id);
        // A jump, a long inactive tab, or an instantaneous end event earns no watched time.
        double delta=position-last;
        double credit=input.playing()&&elapsed<=30&&delta>0&&delta<=elapsed*2.25+1?Math.min(delta,elapsed*2):0;
        // Serialize all sessions for this account/lesson; replays cannot repeatedly credit earlier positions.
        jdbc.update("INSERT INTO learning_lesson_progress(student_id,lesson_id) VALUES (?,?) ON CONFLICT DO NOTHING",user.id(),id);
        var previous=jdbc.queryForMap("SELECT position_seconds,watched_seconds FROM learning_lesson_progress WHERE student_id=? AND lesson_id=? FOR UPDATE",user.id(),id);
        double frontier=((Number)previous.get("watched_seconds")).doubleValue();
        double valid=credit>0&&last<=frontier+2?Math.max(0,Math.min(credit,position-frontier)):0;
        double watched=Math.min(duration,frontier+valid);
        jdbc.update("UPDATE learning_lesson_progress SET position_seconds=?,watched_seconds=?,completed_at=CASE WHEN ?>=?*0.9 THEN COALESCE(completed_at,CURRENT_TIMESTAMP) ELSE completed_at END,updated_at=CURRENT_TIMESTAMP WHERE student_id=? AND lesson_id=?",position,watched,watched,duration,user.id(),id);
        jdbc.update("UPDATE learning_playback_sessions SET last_position=?,last_event_at=CURRENT_TIMESTAMP WHERE id=?",position,input.sessionId());
        return progress(user.id(),id);
    }
    private Map<String,Object> progress(long student,long lesson) {return jdbc.queryForList("SELECT position_seconds,watched_seconds,completed_at FROM learning_lesson_progress WHERE student_id=? AND lesson_id=?",student,lesson).stream().findFirst().orElse(Map.of("position_seconds",0,"watched_seconds",0));}
    private Map<String,Object> ticket(UUID ticket) {
        var rows=jdbc.queryForList("SELECT s.lesson_id,s.preview,u.email FROM learning_playback_sessions s JOIN student_users u ON u.id=s.student_id WHERE s.id=? AND s.expires_at>CURRENT_TIMESTAMP AND s.token_version=u.token_version AND u.account_status='ACTIVE' AND u.email_verified=TRUE",ticket);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Reprodução expirada ou indisponível.");return rows.get(0);
    }
}
