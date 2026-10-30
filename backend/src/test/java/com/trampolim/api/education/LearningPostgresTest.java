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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockMultipartFile;

class LearningPostgresTest {
    private static JdbcTemplate admin,jdbc;
    private static String schema;
    private LearningAccess access;private LearningController controller;private LessonMediaStore store;
    private long teacher,student,outsider;private String teacherEmail,studentEmail,outsiderEmail;
    @BeforeAll static void database() {
        String url=env("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db");
        String user=env("CAPACITA_DB_USERNAME","trampolim_capacita"),password=env("CAPACITA_DB_PASSWORD","dev_capacita_only");
        admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));schema="video_test_"+UUID.randomUUID().toString().replace("-","");admin.execute("CREATE SCHEMA "+schema);
        var source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);
        Flyway.configure().dataSource(source).locations("classpath:db/migration-capacita").load().migrate();jdbc=new JdbcTemplate(source);
    }
    @AfterAll static void cleanup() {if(admin!=null&&schema!=null) admin.execute("DROP SCHEMA "+schema+" CASCADE");}
    @BeforeEach void setup() {
        teacherEmail="teacher-"+UUID.randomUUID()+"@example.test";studentEmail="student-"+UUID.randomUUID()+"@example.test";outsiderEmail="other-"+UUID.randomUUID()+"@example.test";
        teacher=account(teacherEmail);student=account(studentEmail);outsider=account(outsiderEmail);
        jdbc.update("INSERT INTO learning_account_roles(user_id,role) VALUES (?,'TEACHER'),(?,'TEACHER')",teacher,outsider);
        access=new LearningAccess(jdbc);ReflectionTestUtils.setField(access,"bootstrapAdmins","");controller=new LearningController(jdbc,access);store=new LessonMediaStore();
    }
    private long account(String email) {return jdbc.queryForObject("INSERT INTO student_users(email,password_hash,full_name,education_stage,email_verified,account_status,terms_accepted_at) VALUES (?,'test-only','Fixture','HIGH_SCHOOL',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",Long.class,email);}
    private long lesson() {
        long course=((Number)controller.createCourse(teacherEmail,new LearningController.CourseInput("Curso teste","Descrição","Matemática",null,"PUBLISHED")).get("id")).longValue();
        long subject=((Number)controller.createSubject(teacherEmail,course,new LearningController.SectionInput("Disciplina","Descrição",0)).get("id")).longValue();
        long module=((Number)controller.createModule(teacherEmail,subject,new LearningController.SectionInput("Módulo","Descrição",0)).get("id")).longValue();
        return ((Number)controller.createLesson(teacherEmail,module,new LearningController.LessonInput("Aula","Descrição","Objetivo","Texto","WEBVTT\n",0,"DRAFT")).get("id")).longValue();
    }
    @Test void hierarchyAndOwnershipAndPublishingGate() {
        long id=lesson();long course=access.courseForLesson(id);
        assertEquals(1,((List<?>)controller.course(teacherEmail,course,true).get("subjects")).size());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.editLesson(outsiderEmail,id,new LearningController.LessonInput("Invasão","","","","",0,"DRAFT"))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.createCourse(studentEmail,new LearningController.CourseInput("x","x","x",null,"DRAFT"))).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->controller.editLesson(teacherEmail,id,new LearningController.LessonInput("Aula","","","","",0,"PUBLISHED"))).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.lesson(studentEmail,id,false)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.studentProgress(outsiderEmail,course)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.role(studentEmail,new LearningController.RoleInput(studentEmail,"ADMIN",true))).getStatusCode().value());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM learning_account_roles WHERE user_id=?",Integer.class,student));
        jdbc.update("INSERT INTO learning_account_roles(user_id,role) VALUES (?,'ADMIN')",outsider);
        controller.moderate(outsiderEmail,course,new LearningController.StatusInput("HIDDEN"));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.editCourse(teacherEmail,course,new LearningController.CourseInput("Tentativa","","Categoria",null,"PUBLISHED"))).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.course(studentEmail,course,false)).getStatusCode().value());
        var manager=new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource()));
        var media=new LessonMediaController(jdbc,access,controller,store,manager);
        var attempt=new MockMultipartFile("file","attempt.mp4","video/mp4",new byte[]{1});
        jdbc.update("INSERT INTO learning_upload_limits(user_id,attempts) VALUES (?,12)",teacher);
        assertEquals(429,assertThrows(ResponseStatusException.class,()->media.upload(teacherEmail,id,attempt)).getStatusCode().value());
        jdbc.update("UPDATE learning_upload_limits SET attempts=0 WHERE user_id=?",teacher);
        jdbc.update("UPDATE learning_lessons SET media_bytes=536870912 WHERE id=?",id);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->media.upload(teacherEmail,id,attempt)).getStatusCode().value());
    }
    @Test void durationBoundariesAreStrict() {
        for(double duration:new double[]{180,240,300}) assertDoesNotThrow(()->LessonMediaStore.validateDuration(duration));
        for(double duration:new double[]{179.99,300.01,-1,Double.NaN,Double.POSITIVE_INFINITY}) assertThrows(ResponseStatusException.class,()->LessonMediaStore.validateDuration(duration));
        assertThrows(ResponseStatusException.class,()->store.store(new MockMultipartFile("file","malicious.exe","video/mp4",new byte[]{1,2,3})));
    }
    @Test void progressRejectsSeekingAndRequiresRealElapsedTimeAndOwnSession() {
        long id=lesson();jdbc.update("UPDATE learning_lessons SET media_key=?,duration_seconds=200,validation_status='READY',status='PUBLISHED' WHERE id=?",UUID.randomUUID(),id);
        var manager=new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource()));var tx=new TransactionTemplate(manager);
        var media=new LessonMediaController(jdbc,access,controller,store,manager);var tokens=new com.trampolim.api.security.TrailTokenService();ReflectionTestUtils.setField(tokens,"capacitaSecret","learning-test-signing-key-0123456789-abcdefghijklmnopqrstuvwxyz");var playback=new LearningPlaybackController(jdbc,access,controller,media,tokens);
        UUID session=(UUID)tx.execute(status->playback.start(studentEmail,id,false,"Bearer "+tokens.issue(com.trampolim.api.security.TrailTokenService.Trail.CAPACITA,studentEmail,student,0))).get("sessionId");
        jdbc.update("UPDATE learning_playback_sessions SET last_event_at=CURRENT_TIMESTAMP-INTERVAL '5 seconds' WHERE id=?",session);
        tx.execute(status->playback.update(studentEmail,id,new LearningPlaybackController.PlaybackInput(session,200,true)));
        assertNull(jdbc.queryForObject("SELECT completed_at FROM learning_lesson_progress WHERE student_id=? AND lesson_id=?",java.sql.Timestamp.class,student,id));
        assertEquals(0,jdbc.queryForObject("SELECT watched_seconds FROM learning_lesson_progress WHERE student_id=? AND lesson_id=?",Double.class,student,id));
        assertThrows(ResponseStatusException.class,()->tx.execute(status->playback.update(outsiderEmail,id,new LearningPlaybackController.PlaybackInput(session,10,true))));
        jdbc.update("UPDATE learning_lesson_progress SET watched_seconds=175,position_seconds=175 WHERE student_id=? AND lesson_id=?",student,id);
        jdbc.update("UPDATE learning_playback_sessions SET last_position=175,last_event_at=CURRENT_TIMESTAMP-INTERVAL '5 seconds' WHERE id=?",session);
        tx.execute(status->playback.update(studentEmail,id,new LearningPlaybackController.PlaybackInput(session,180,true)));
        assertNotNull(jdbc.queryForObject("SELECT completed_at FROM learning_lesson_progress WHERE student_id=? AND lesson_id=?",java.sql.Timestamp.class,student,id));
        assertEquals(1,controller.studentProgress(teacherEmail,access.courseForLesson(id)).size());
    }
    @Test void realVideoProcessingAndPublication() throws Exception {
        String ffmpeg=System.getenv("CAPACITA_FFMPEG"),ffprobe=System.getenv("CAPACITA_FFPROBE");
        Assumptions.assumeTrue(ffmpeg!=null&&ffprobe!=null,"Configure FFmpeg/FFprobe para o teste real de vídeos.");
        Path temporary=Files.createTempDirectory("trampolim-video-test-");
        ReflectionTestUtils.setField(store,"root",temporary.resolve("private").toString());ReflectionTestUtils.setField(store,"ffmpeg",ffmpeg);ReflectionTestUtils.setField(store,"ffprobe",ffprobe);ReflectionTestUtils.setField(store,"maxBytes",134217728L);
        try {
            for(int duration:new int[]{179,180,300,301}) {
                Path source=temporary.resolve("fixture-"+duration+".mp4");
                var process=new ProcessBuilder(ffmpeg,"-nostdin","-v","error","-f","lavfi","-i","color=c=blue:s=160x90:r=2","-t",Integer.toString(duration),"-c:v","libx264","-pix_fmt","yuv420p","-y",source.toString()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                assertTrue(process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());
                var file=new MockMultipartFile("file",source.getFileName().toString(),"video/mp4",Files.readAllBytes(source));
                if(duration<180||duration>300) assertThrows(ResponseStatusException.class,()->store.store(file));
                else {
                    long id=lesson();var manager=new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource()));
                    var media=new LessonMediaController(jdbc,access,controller,store,manager);
                    assertEquals("READY",media.upload(teacherEmail,id,file).get("validationStatus"));
                    controller.editLesson(teacherEmail,id,new LearningController.LessonInput("Aula publicada","Descrição","Objetivo","Texto","WEBVTT\n",0,"PUBLISHED"));
                    assertEquals("PUBLISHED",controller.lesson(studentEmail,id,false).get("status"));
                    assertTrue(media.video(studentEmail,id,false).getBody().exists());
                    assertThrows(ResponseStatusException.class,()->media.upload(outsiderEmail,id,file));
                }
            }
            assertThrows(ResponseStatusException.class,()->store.store(new MockMultipartFile("file","fake.mp4","video/mp4","not-a-video".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            for(String extension:List.of("mov","webm")) {
                Path source=temporary.resolve("fixture."+extension);
                var args=new ArrayList<>(List.of(ffmpeg,"-nostdin","-v","error","-f","lavfi","-i","color=c=blue:s=160x90:r=2","-t","180","-c:v",extension.equals("webm")?"libvpx-vp9":"libx264","-y",source.toString()));
                var process=new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                assertTrue(process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());
                var converted=store.store(new MockMultipartFile("file",source.getFileName().toString(),extension.equals("webm")?"video/webm":"video/quicktime",Files.readAllBytes(source)));
                assertTrue(converted.duration()>=180&&converted.duration()<=300);assertTrue(Files.exists(store.file(converted.key())));
            }
        } finally {try(var paths=Files.walk(temporary)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);}}
    }
    private static String env(String name,String fallback) {String value=System.getenv(name);return value==null||value.isBlank()?fallback:value;}
}
