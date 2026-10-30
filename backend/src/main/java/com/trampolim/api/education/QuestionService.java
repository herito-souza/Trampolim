package com.trampolim.api.education;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class QuestionService {
    private final JdbcTemplate jdbc;private final LearningAccess access;private final LearningController learning;
    @Value("${capacita.questions.max-questions-hour:10}") private int maxQuestions=10;
    @Value("${capacita.questions.max-messages-hour:60}") private int maxMessages=60;
    @Value("${capacita.questions.max-teacher-messages-hour:300}") private int maxTeacherMessages=300;
    public QuestionService(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc,LearningAccess access,LearningController learning) {this.jdbc=jdbc;this.access=access;this.learning=learning;}
    public record MessageInput(UUID clientKey,String body,UUID mediaId,String transcript,String captionsVtt) {}
    public record QuestionInput(UUID clientKey,long courseId,Long moduleId,Long lessonId,String title,String body,UUID mediaId,String transcript,String captionsVtt) {}
    public record BatchInput(UUID clientKey,List<Long> questionIds,String body,UUID mediaId,String transcript,String captionsVtt,boolean confirmed) {}
    public record BatchResult(UUID id,List<Long> questionIds,String createdAt) {}
    private static final String QUESTION_SQL="SELECT q.id,q.course_id,q.module_id,q.lesson_id,q.student_id,q.title,q.status,q.created_at,q.updated_at,q.closed_at,c.teacher_id,c.title AS course_title,m.title AS module_title,l.title AS lesson_title,u.full_name AS student_name FROM learning_questions q JOIN learning_courses c ON c.id=q.course_id JOIN student_users u ON u.id=q.student_id LEFT JOIN learning_modules m ON m.id=q.module_id LEFT JOIN learning_lessons l ON l.id=q.lesson_id";
    public List<Map<String,Object>> list(String email,boolean inbox,Long course,Long module,Long lesson,String status,String order,int offset) {
        var user=access.account(email);var args=new ArrayList<Object>();StringBuilder sql=new StringBuilder(QUESTION_SQL+" WHERE ");
        if(inbox) {if(!user.teacher()) denied();if(user.admin()) sql.append("TRUE");else {sql.append("c.teacher_id=?");args.add(user.id());}}
        else {sql.append("q.student_id=?");args.add(user.id());}
        if(course!=null){sql.append(" AND q.course_id=?");args.add(course);}if(module!=null){sql.append(" AND q.module_id=?");args.add(module);}if(lesson!=null){sql.append(" AND q.lesson_id=?");args.add(lesson);}
        if(status!=null&&!status.isBlank()){validStatus(status);sql.append(" AND q.status=?");args.add(status);}
        if(offset<0||offset>100000) bad("Página inválida.");
        sql.append(" ORDER BY q.created_at ").append("OLDEST".equals(order)?"ASC":"DESC").append(",q.id DESC LIMIT 50 OFFSET ?");args.add(offset);
        return jdbc.queryForList(sql.toString(),args.toArray());
    }
    public Map<String,Object> question(LearningAccess.Account user,long id,boolean lock) {
        var rows=jdbc.queryForList(QUESTION_SQL+" WHERE q.id=?"+(lock?" FOR UPDATE OF q":""),id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Dúvida indisponível.");
        var row=rows.get(0);if(number(row,"student_id")!=user.id()&&!canTeach(user,row)) denied();return row;
    }
    public Map<String,Object> detail(String email,long id) {return question(access.account(email),id,false);}
    public List<Map<String,Object>> messages(String email,long id,Long before) {
        question(access.account(email),id,false);
        return jdbc.queryForList("SELECT m.id,m.question_id,m.author_id,m.author_role,m.body,m.media_id,m.transcript,m.captions_vtt,m.created_at,COALESCE(u.full_name,'Conta removida') AS author_name,a.kind AS media_kind,a.duration_seconds FROM learning_question_messages m LEFT JOIN student_users u ON u.id=m.author_id LEFT JOIN learning_question_media a ON a.id=m.media_id WHERE m.question_id=? AND m.id<? ORDER BY m.id DESC LIMIT 50",id,before==null?Long.MAX_VALUE:before);
    }
    @Transactional(transactionManager="capacitaTransactionManager")
    public Map<String,Object> create(String email,QuestionInput input) {
        var user=access.account(email);if(input==null||input.clientKey()==null) bad("Informe a chave de envio.");required(input.title(),180);
        var message=new MessageInput(input.clientKey(),input.body(),input.mediaId(),input.transcript(),input.captionsVtt());validate(message);
        String fingerprint=hashFields(input.courseId(),input.moduleId(),input.lessonId(),input.title().trim(),fingerprint(message));
        lockAccount(user.id());var existing=jdbc.queryForList("SELECT id,request_hash FROM learning_questions WHERE student_id=? AND client_key=?",user.id(),input.clientKey());
        if(!existing.isEmpty()){same(fingerprint,existing.get(0).get("request_hash"));return Map.of("id",existing.get(0).get("id"));}
        learning.course(email,input.courseId(),false);
        if(input.moduleId()!=null&&access.courseForModule(input.moduleId())!=input.courseId()) bad("O módulo não pertence ao curso.");
        if(input.lessonId()!=null) {
            if(input.moduleId()==null||access.courseForLesson(input.lessonId())!=input.courseId()||jdbc.queryForObject("SELECT module_id FROM learning_lessons WHERE id=?",Long.class,input.lessonId()).longValue()!=input.moduleId()) bad("A aula não pertence ao módulo informado.");
            learning.visibleLesson(user,input.lessonId(),false);
        }
        if(jdbc.queryForObject("SELECT count(*) FROM learning_questions WHERE student_id=? AND created_at>CURRENT_TIMESTAMP-INTERVAL '1 hour'",Integer.class,user.id())>=maxQuestions) limited();
        rate(user,1,false);mediaOwner(user,message.mediaId());
        Long id=jdbc.queryForObject("INSERT INTO learning_questions(course_id,module_id,lesson_id,student_id,title,client_key,request_hash) VALUES (?,?,?,?,?,?,?) RETURNING id",Long.class,input.courseId(),input.moduleId(),input.lessonId(),user.id(),input.title().trim(),input.clientKey(),fingerprint);
        long messageId=insertMessage(user,id,message,false,null);
        var thread=question(user,id,false);notify(number(thread,"teacher_id"),user.id(),id,messageId,"QUESTION");audit(user.id(),id,"CREATED");return Map.of("id",id);
    }
    @Transactional(transactionManager="capacitaTransactionManager")
    public Map<String,Object> send(String email,long id,MessageInput input,boolean teacherReply) {
        var user=access.account(email);validate(input);lockAccount(user.id());var thread=question(user,id,true);
        if(teacherReply&&!canTeach(user,thread)) denied();if(!teacherReply&&number(thread,"student_id")!=user.id()) denied();
        var old=jdbc.queryForList("SELECT id,request_hash FROM learning_question_messages WHERE question_id=? AND author_id=? AND client_key=?",id,user.id(),input.clientKey());
        if(!old.isEmpty()){same(fingerprint(input),old.get(0).get("request_hash"));return Map.of("id",old.get(0).get("id"));}
        if("CLOSED".equals(thread.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Este atendimento foi encerrado. O professor pode reabri-lo; você também pode criar uma nova dúvida.");
        rate(user,1,teacherReply);mediaOwner(user,input.mediaId());long message=insertMessage(user,id,input,teacherReply,null);
        jdbc.update("UPDATE learning_questions SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",teacherReply?"ANSWERED":"WAITING",id);
        notify(number(thread,teacherReply?"student_id":"teacher_id"),user.id(),id,message,teacherReply?"REPLY":"FOLLOW_UP");audit(user.id(),id,teacherReply?"REPLIED":"FOLLOW_UP");return Map.of("id",message);
    }
    @Transactional(transactionManager="capacitaTransactionManager")
    public BatchResult batch(String email,BatchInput input) {
        var user=access.teacher(email);
        if(input==null||input.clientKey()==null||!input.confirmed()||input.questionIds()==null||input.questionIds().isEmpty()||input.questionIds().size()>50) bad("Revise e confirme de 1 a 50 dúvidas antes do envio.");
        var ids=input.questionIds().stream().filter(Objects::nonNull).distinct().sorted().toList();if(ids.size()!=input.questionIds().size()) bad("A seleção contém dúvidas inválidas ou repetidas.");
        var message=new MessageInput(input.clientKey(),input.body(),input.mediaId(),input.transcript(),input.captionsVtt());validate(message);String fingerprint=hashFields(ids,fingerprint(message));
        lockAccount(user.id());var previous=jdbc.queryForList("SELECT id,request_hash FROM learning_reply_batches WHERE author_id=? AND client_key=?",user.id(),input.clientKey());
        if(!previous.isEmpty()){same(fingerprint,previous.get(0).get("request_hash"));return batchResult(user.id(),(UUID)previous.get(0).get("id"));}
        var threads=new ArrayList<Map<String,Object>>();
        for(long id:ids){var thread=question(user,id,true);if(!canTeach(user,thread)) denied();if(!Set.of("WAITING","IN_PROGRESS").contains(thread.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Uma das dúvidas não está mais pendente. Revise a seleção.");if(jdbc.queryForObject("SELECT count(*) FROM learning_question_messages WHERE question_id=? AND author_id=? AND client_key=?",Integer.class,id,user.id(),input.clientKey())>0) bad("Esta chave já foi usada em uma resposta individual.");threads.add(thread);}
        rate(user,ids.size(),true);mediaOwner(user,input.mediaId());UUID batch=UUID.randomUUID();
        jdbc.update("INSERT INTO learning_reply_batches(id,author_id,client_key,request_hash,question_ids) VALUES (?,?,?,?,?)",batch,user.id(),input.clientKey(),fingerprint,ids.toArray(Long[]::new));
        for(var thread:threads){long id=number(thread,"id");long messageId=insertMessage(user,id,message,true,batch);jdbc.update("UPDATE learning_questions SET status='ANSWERED',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);notify(number(thread,"student_id"),user.id(),id,messageId,"REPLY");audit(user.id(),id,"BATCH_REPLIED");}
        return batchResult(user.id(),batch);
    }
    public List<BatchResult> batches(String email) {var user=access.teacher(email);return jdbc.query("SELECT id,question_ids,created_at FROM learning_reply_batches WHERE author_id=? ORDER BY created_at DESC LIMIT 50",(rs,row)->{var array=rs.getArray(2);try{return new BatchResult((UUID)rs.getObject(1),Arrays.asList((Long[])array.getArray()),rs.getTimestamp(3).toInstant().toString());}finally{array.free();}},user.id());}
    private BatchResult batchResult(long user,UUID id) {return batchesById(user,id).get(0);}
    private List<BatchResult> batchesById(long user,UUID id) {return jdbc.query("SELECT id,question_ids,created_at FROM learning_reply_batches WHERE author_id=? AND id=?",(rs,row)->{var array=rs.getArray(2);try{return new BatchResult((UUID)rs.getObject(1),Arrays.asList((Long[])array.getArray()),rs.getTimestamp(3).toInstant().toString());}finally{array.free();}},user,id);}
    @Transactional(transactionManager="capacitaTransactionManager")
    public void status(String email,long id,String status) {
        var user=access.teacher(email);var thread=question(user,id,true);if(!canTeach(user,thread)) denied();
        if(status==null||!Set.of("WAITING","IN_PROGRESS","CLOSED").contains(status)) bad("Escolha aguardando, em atendimento ou encerrada.");
        if(status.equals(thread.get("status"))) return;
        jdbc.update("UPDATE learning_questions SET status=?,closed_at=CASE WHEN ?='CLOSED' THEN CURRENT_TIMESTAMP ELSE NULL END,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,status,id);
        notify(number(thread,"student_id"),user.id(),id,null,"STATUS");audit(user.id(),id,"STATUS_"+status);
    }
    @Transactional(transactionManager="capacitaTransactionManager")
    public void delete(String email,long id) {var user=access.account(email);var thread=question(user,id,true);if(number(thread,"student_id")!=user.id()&&!user.admin()) denied();audit(user.id(),id,"DELETED");jdbc.update("DELETE FROM learning_questions WHERE id=?",id);}
    public void mediaAccess(LearningAccess.Account user,UUID media) {
        var owners=jdbc.queryForList("SELECT owner_id FROM learning_question_media WHERE id=?",Long.class,media);if(owners.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(owners.get(0)==user.id()||user.admin()) return;
        if(jdbc.queryForObject("SELECT count(*) FROM learning_question_messages m JOIN learning_questions q ON q.id=m.question_id JOIN learning_courses c ON c.id=q.course_id WHERE m.media_id=? AND (q.student_id=? OR (c.teacher_id=? AND ?))",Integer.class,media,user.id(),user.id(),user.teacher())==0) denied();
    }
    private boolean canTeach(LearningAccess.Account user,Map<String,Object> thread) {return user.admin()||(user.teacher()&&number(thread,"teacher_id")==user.id());}
    private long insertMessage(LearningAccess.Account user,long id,MessageInput input,boolean teacher,UUID batch) {return jdbc.queryForObject("INSERT INTO learning_question_messages(question_id,author_id,author_role,body,media_id,transcript,captions_vtt,client_key,request_hash,batch_id) VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,id,user.id(),teacher?"TEACHER":"STUDENT",text(input.body(),6000),input.mediaId(),text(input.transcript(),20000),captions(input.captionsVtt()),input.clientKey(),fingerprint(input),batch);}
    private void notify(long receiver,long actor,long thread,Long message,String kind) {if(receiver!=actor) jdbc.update("INSERT INTO learning_notifications(user_id,question_id,message_id,kind) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",receiver,thread,message,kind);}
    private void audit(long actor,long id,String event) {jdbc.update("INSERT INTO learning_question_audit(actor_id,question_id,event) VALUES (?,?,?)",actor,id,event);}
    private void lockAccount(long id) {jdbc.queryForObject("SELECT id FROM student_users WHERE id=? FOR NO KEY UPDATE",Long.class,id);}
    private void mediaOwner(LearningAccess.Account user,UUID id) {if(id==null)return;var owners=jdbc.query("SELECT owner_id FROM learning_question_media WHERE id=? FOR UPDATE",(rs,row)->rs.getLong(1),id);if(owners.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Mídia indisponível.");if(owners.get(0)!=user.id()) denied();jdbc.update("UPDATE learning_question_media SET last_attached_at=CURRENT_TIMESTAMP WHERE id=?",id);}
    private void rate(LearningAccess.Account user,int adding,boolean teacher) {int count=jdbc.queryForObject("SELECT count(*) FROM learning_question_messages WHERE author_id=? AND created_at>CURRENT_TIMESTAMP-INTERVAL '1 hour'",Integer.class,user.id());if(count+adding>(teacher?maxTeacherMessages:maxMessages))limited();}
    private void validate(MessageInput input) {if(input==null||input.clientKey()==null)bad("Informe a chave do envio.");if(text(input.body(),6000).isEmpty()&&input.mediaId()==null)bad("Escreva uma mensagem ou anexe áudio/vídeo.");text(input.transcript(),20000);captions(input.captionsVtt());}
    private String fingerprint(MessageInput input) {return hashFields(text(input.body(),6000),input.mediaId(),text(input.transcript(),20000),captions(input.captionsVtt()));}
    private String hashFields(Object... fields) {try{return hash(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(fields));}catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new IllegalStateException("Falha ao preparar a chave de envio.");}}
    private String captions(String input) {String value=text(input,50000);if(!value.isEmpty()&&!value.startsWith("WEBVTT"))bad("Legendas devem estar em WEBVTT.");return value;}
    private String text(String input,int max) {String value=input==null?"":input.trim();if(value.length()>max)bad("Texto acima do limite permitido.");return value;}
    private void required(String value,int max) {if(value==null||value.isBlank()||value.length()>max)bad("Informe um título de até 180 caracteres.");}
    private void validStatus(String status) {if(!Set.of("WAITING","IN_PROGRESS","ANSWERED","CLOSED").contains(status))bad("Situação inválida.");}
    private long number(Map<String,Object> row,String name) {return ((Number)row.get(name)).longValue();}
    private void same(String actual,Object expected) {if(!actual.equals(expected))throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta chave de envio já foi utilizada com outros dados.");}
    private String hash(String value) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}}
    private void bad(String message) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private void denied() {throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta dúvida é privada e você não tem acesso.");}
    private void limited() {throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite de mensagens atingido. Aguarde antes de enviar novamente.");}
}
