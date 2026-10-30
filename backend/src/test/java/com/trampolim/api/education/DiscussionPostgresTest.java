package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class DiscussionPostgresTest {
    static JdbcTemplate admin,jdbc;static String schema;QuestionService service;LearningAccess access;LearningController learning;TransactionTemplate tx;
    String teacherEmail,studentEmail,secondEmail,otherEmail;long teacher,student,second,other,course,module,lesson;
    @BeforeAll static void database(){String url=env("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db");String user=env("CAPACITA_DB_USERNAME","trampolim_capacita"),password=env("CAPACITA_DB_PASSWORD","dev_capacita_only");admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));schema="discussion_test_"+UUID.randomUUID().toString().replace("-","");admin.execute("CREATE SCHEMA "+schema);var source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);Flyway.configure().dataSource(source).locations("classpath:db/migration-capacita").load().migrate();jdbc=new JdbcTemplate(source);}
    @AfterAll static void cleanup(){if(admin!=null&&schema!=null)admin.execute("DROP SCHEMA "+schema+" CASCADE");}
    @BeforeEach void setup(){teacherEmail=email();studentEmail=email();secondEmail=email();otherEmail=email();teacher=account(teacherEmail);student=account(studentEmail);second=account(secondEmail);other=account(otherEmail);jdbc.update("INSERT INTO learning_account_roles(user_id,role) VALUES (?,'TEACHER'),(?,'TEACHER')",teacher,other);access=new LearningAccess(jdbc);ReflectionTestUtils.setField(access,"bootstrapAdmins","");learning=new LearningController(jdbc,access);service=new QuestionService(jdbc,access,learning);tx=new TransactionTemplate(new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource())));course=((Number)learning.createCourse(teacherEmail,new LearningController.CourseInput("Course","Description","Category",null,"PUBLISHED")).get("id")).longValue();module=module(course);lesson=jdbc.queryForObject("INSERT INTO learning_lessons(module_id,title,status,media_key,duration_seconds,validation_status) VALUES (?,'Lesson','PUBLISHED',?,180,'READY') RETURNING id",Long.class,module,UUID.randomUUID());}
    private long account(String email){return jdbc.queryForObject("INSERT INTO student_users(email,password_hash,full_name,education_stage,email_verified,account_status,terms_accepted_at) VALUES (?,'test-only','Private fixture','HIGH_SCHOOL',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",Long.class,email);}
    private long module(long course){long subject=jdbc.queryForObject("INSERT INTO learning_subjects(course_id,title) VALUES (?,'Subject') RETURNING id",Long.class,course);return jdbc.queryForObject("INSERT INTO learning_modules(subject_id,title) VALUES (?,'Module') RETURNING id",Long.class,subject);}
    private QuestionService.QuestionInput input(UUID key,long course,Long module,Long lesson,String title,String body,UUID media){return new QuestionService.QuestionInput(key,course,module,lesson,title,body,media,"","");}
    private long create(String email,QuestionService.QuestionInput input){return ((Number)tx.execute(status->service.create(email,input)).get("id")).longValue();}
    private QuestionService.MessageInput message(UUID key,String body,UUID media){return new QuestionService.MessageInput(key,body,media,"","");}
    @Test void textQuestionsPrivacyStatesAndIdempotence(){
        var input=input(UUID.randomUUID(),course,module,lesson,"Private question","Explain this",null);long id=create(studentEmail,input);assertEquals(id,create(studentEmail,input));
        assertEquals("WAITING",service.detail(studentEmail,id).get("status"));assertEquals(1,service.messages(studentEmail,id,null).size());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM learning_notifications WHERE user_id=?",Integer.class,teacher));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.detail(secondEmail,id)).getStatusCode().value());assertThrows(ResponseStatusException.class,()->service.messages(otherEmail,id,null));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->create(studentEmail,input(input.clientKey(),course,module,lesson,"Different title","Explain this",null))).getStatusCode().value());
        var reply=message(UUID.randomUUID(),"Private answer",null);var first=tx.execute(status->service.send(teacherEmail,id,reply,true));var repeated=tx.execute(status->service.send(teacherEmail,id,reply,true));assertEquals(first,repeated);assertEquals("ANSWERED",service.detail(studentEmail,id).get("status"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM learning_notifications WHERE user_id=?",Integer.class,student));
        tx.executeWithoutResult(status->service.status(teacherEmail,id,"CLOSED"));assertThrows(ResponseStatusException.class,()->tx.execute(status->service.send(studentEmail,id,message(UUID.randomUUID(),"Follow up",null),false)));
        tx.executeWithoutResult(status->service.status(teacherEmail,id,"WAITING"));tx.execute(status->service.send(studentEmail,id,message(UUID.randomUUID(),"Follow up",null),false));assertEquals("WAITING",service.detail(studentEmail,id).get("status"));
        var notices=new LearningNotifications(jdbc,access,org.mockito.Mockito.mock(com.trampolim.api.security.TrailTokenService.class));try {var rows=(List<?>)notices.history(studentEmail,null).get("items");assertFalse(rows.isEmpty());long notification=((Number)((Map<?,?>)rows.get(0)).get("id")).longValue();assertThrows(ResponseStatusException.class,()->notices.read(secondEmail,notification));notices.read(studentEmail,notification);assertNotNull(jdbc.queryForObject("SELECT read_at FROM learning_notifications WHERE id=?",java.sql.Timestamp.class,notification));}finally{notices.shutdown();}
    }
    @Test void concurrentBatchHasOneReceiptAndOneAnswerPerQuestion() throws Exception {
        long first=create(studentEmail,input(UUID.randomUUID(),course,null,null,"First","Similar question",null));long next=create(secondEmail,input(UUID.randomUUID(),course,module,null,"Second","Similar question",null));
        var key=UUID.randomUUID();var unconfirmed=new QuestionService.BatchInput(key,List.of(first,next),"General answer",null,"","",false);assertThrows(ResponseStatusException.class,()->tx.execute(status->service.batch(teacherEmail,unconfirmed)));
        var payload=new QuestionService.BatchInput(key,List.of(next,first),"General answer",null,"","",true);
        var a=java.util.concurrent.CompletableFuture.supplyAsync(()->tx.execute(status->service.batch(teacherEmail,payload)));var b=java.util.concurrent.CompletableFuture.supplyAsync(()->tx.execute(status->service.batch(teacherEmail,payload)));
        assertEquals(a.get().id(),b.get().id());assertEquals(1,service.batches(teacherEmail).size());
        for(long id:List.of(first,next)){assertEquals("ANSWERED",jdbc.queryForObject("SELECT status FROM learning_questions WHERE id=?",String.class,id));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM learning_question_messages WHERE question_id=? AND author_role='TEACHER'",Integer.class,id));}
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM learning_notifications WHERE user_id=? AND kind='REPLY'",Integer.class,student));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM learning_notifications WHERE user_id=? AND kind='REPLY'",Integer.class,second));
        assertThrows(ResponseStatusException.class,()->service.messages(secondEmail,first,null));assertFalse(service.messages(studentEmail,first,null).get(0).containsKey("batch_id"));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->tx.execute(status->service.batch(teacherEmail,new QuestionService.BatchInput(key,List.of(first),"Changed",null,"","",true)))).getStatusCode().value());
    }
    @Test void invalidBatchRollsBackAndCannotReuseStudentRecording(){
        long own=create(studentEmail,input(UUID.randomUUID(),course,null,null,"Own course","Question",null));long otherCourse=((Number)learning.createCourse(otherEmail,new LearningController.CourseInput("Other","","Category",null,"PUBLISHED")).get("id")).longValue();long foreign=create(secondEmail,input(UUID.randomUUID(),otherCourse,null,null,"Foreign","Question",null));
        assertThrows(ResponseStatusException.class,()->tx.execute(status->service.batch(teacherEmail,new QuestionService.BatchInput(UUID.randomUUID(),List.of(own,foreign),"Answer",null,"","",true))));assertEquals("WAITING",service.detail(studentEmail,own).get("status"));assertEquals(0,service.batches(teacherEmail).size());
        UUID audio=UUID.randomUUID();jdbc.update("INSERT INTO learning_question_media(id,owner_id,storage_key,kind,media_type,duration_seconds,size_bytes) VALUES (?,?,?,'AUDIO','audio/mp4',3,100)",audio,student,UUID.randomUUID());
        long recorded=create(studentEmail,input(UUID.randomUUID(),course,module,null,"Audio question","",audio));assertEquals(audio,service.messages(studentEmail,recorded,null).get(0).get("media_id"));
        assertThrows(ResponseStatusException.class,()->tx.execute(status->service.send(teacherEmail,own,message(UUID.randomUUID(),"Forwarded",audio),true)));assertThrows(ResponseStatusException.class,()->service.mediaAccess(access.account(secondEmail),audio));service.mediaAccess(access.account(teacherEmail),audio);
        assertThrows(ResponseStatusException.class,()->create(secondEmail,input(UUID.randomUUID(),course,null,null,"Not mine","",audio)));
        tx.executeWithoutResult(status->service.delete(studentEmail,recorded));assertThrows(ResponseStatusException.class,()->service.mediaAccess(access.account(teacherEmail),audio));
    }
    @Test void simultaneousStudentAndTeacherMessagesDoNotDeadlock() throws Exception {
        long id=create(studentEmail,input(UUID.randomUUID(),course,null,null,"Concurrent conversation","Question",null));
        var barrier=new java.util.concurrent.CyclicBarrier(2);
        var learner=java.util.concurrent.CompletableFuture.supplyAsync(()->tx.execute(status->{jdbc.queryForObject("SELECT id FROM student_users WHERE id=? FOR NO KEY UPDATE",Long.class,student);await(barrier);return service.send(studentEmail,id,message(UUID.randomUUID(),"Follow up",null),false);}));
        var professor=java.util.concurrent.CompletableFuture.supplyAsync(()->tx.execute(status->{jdbc.queryForObject("SELECT id FROM student_users WHERE id=? FOR NO KEY UPDATE",Long.class,teacher);await(barrier);return service.send(teacherEmail,id,message(UUID.randomUUID(),"Answer",null),true);}));
        assertNotNull(learner.get(10,java.util.concurrent.TimeUnit.SECONDS));assertNotNull(professor.get(10,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(3,service.messages(studentEmail,id,null).size());
    }
    private void await(java.util.concurrent.CyclicBarrier barrier){try{barrier.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception error){throw new IllegalStateException(error);}}
    @Test void realAudioVideoLimitsAndPrivateFiles() throws Exception {
        String ffmpeg=System.getenv("CAPACITA_FFMPEG"),ffprobe=System.getenv("CAPACITA_FFPROBE");Assumptions.assumeTrue(ffmpeg!=null&&ffprobe!=null,"Configure FFmpeg e FFprobe para mídias reais.");Path folder=Files.createTempDirectory("question-media-test-");var engine=new LessonMediaStore();ReflectionTestUtils.setField(engine,"root",folder.toString());ReflectionTestUtils.setField(engine,"ffmpeg",ffmpeg);ReflectionTestUtils.setField(engine,"ffprobe",ffprobe);var store=new DiscussionMediaStore(engine);ReflectionTestUtils.setField(store,"root",folder.toString());ReflectionTestUtils.setField(store,"ffmpeg",ffmpeg);
        var manager=new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource()));var tokens=new com.trampolim.api.security.TrailTokenService();ReflectionTestUtils.setField(tokens,"capacitaSecret","discussion-media-test-signing-key-0123456789-abcdefghijklmnopqrstuvwxyz");var media=new DiscussionMediaController(jdbc,access,service,store,manager,tokens);
        try {
            for(String kind:List.of("AUDIO","VIDEO")){
                Path source=folder.resolve(kind.equals("AUDIO")?"audio.wav":"video.mp4");var args=new ArrayList<>(List.of(ffmpeg,"-nostdin","-v","error","-f","lavfi","-i",kind.equals("AUDIO")?"sine=frequency=440:sample_rate=16000":"color=c=blue:s=160x90:r=2","-t","3"));args.addAll(kind.equals("AUDIO")?List.of("-c:a","pcm_s16le"):List.of("-c:v","libx264","-pix_fmt","yuv420p"));args.addAll(List.of("-y",source.toString()));run(args);
                var file=new MockMultipartFile("file",source.getFileName().toString(),kind.equals("AUDIO")?"audio/wav":"video/mp4",Files.readAllBytes(source));var uploaded=media.upload(studentEmail,kind,file);UUID id=(UUID)uploaded.get("id");long question=create(studentEmail,input(UUID.randomUUID(),course,null,null,kind+" question","Optional description",id));
                assertThrows(ResponseStatusException.class,()->media.playback(secondEmail,id,"Bearer "+tokens.issue(com.trampolim.api.security.TrailTokenService.Trail.CAPACITA,secondEmail,second,0)));var ticket=media.playback(teacherEmail,id,"Bearer "+tokens.issue(com.trampolim.api.security.TrailTokenService.Trail.CAPACITA,teacherEmail,teacher,0));String path=(String)ticket.get("path");UUID value=UUID.fromString(path.substring(path.lastIndexOf('/')+1));assertTrue(media.read(value).getBody().exists());assertEquals(kind,service.messages(studentEmail,question,null).get(0).get("media_kind"));
            }
            ReflectionTestUtils.setField(store,"audioSeconds",2);Path source=folder.resolve("audio.wav");assertThrows(ResponseStatusException.class,()->store.store(new MockMultipartFile("file","long.wav","audio/wav",Files.readAllBytes(source)),"AUDIO"));assertThrows(ResponseStatusException.class,()->store.store(new MockMultipartFile("file","fake.mp4","video/mp4",new byte[]{1,2,3}),"VIDEO"));
            ReflectionTestUtils.setField(store,"maximumBytes",1L);assertThrows(ResponseStatusException.class,()->store.store(new MockMultipartFile("file","large.wav","audio/wav",new byte[]{1,2}),"AUDIO"));
        }finally{try(var paths=Files.walk(folder)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    private void run(List<String> args)throws Exception{var process=new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.INHERIT).start();assertTrue(process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());}
    private static String email(){return UUID.randomUUID()+"@example.test";}
    private static String env(String name,String fallback){String value=System.getenv(name);return value==null||value.isBlank()?fallback:value;}
}
