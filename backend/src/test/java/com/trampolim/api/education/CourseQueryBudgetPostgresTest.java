package com.trampolim.api.education;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Real PostgreSQL, synthetic rows and a disposable schema only. */
class CourseQueryBudgetPostgresTest {
    static JdbcTemplate admin; static CountingJdbc jdbc; static String schema;
    static class CountingJdbc extends JdbcTemplate {
        int structures;
        CountingJdbc(javax.sql.DataSource source) { super(source); }
        @Override public List<Map<String,Object>> queryForList(String sql,Object... args) { structures++; return super.queryForList(sql,args); }
    }
    @BeforeAll static void database() {
        String url=env("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db"), user=env("CAPACITA_DB_USERNAME","trampolim_capacita"), password=env("CAPACITA_DB_PASSWORD","dev_capacita_only");
        admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));schema="query_budget_"+UUID.randomUUID().toString().replace("-","");admin.execute("CREATE SCHEMA "+schema);
        var source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);
        Flyway.configure().dataSource(source).locations("classpath:db/migration-capacita").load().migrate();jdbc=new CountingJdbc(source);
    }
    @AfterAll static void cleanup(){if(admin!=null&&schema!=null)admin.execute("DROP SCHEMA "+schema+" CASCADE");}
    @Test void hierarchyQueriesStayConstantAndPreservePublication() throws Exception {
        long teacher=jdbc.queryForObject("INSERT INTO student_users(email,password_hash,full_name,education_stage,email_verified,account_status,terms_accepted_at) VALUES ('query@example.test','synthetic','Fixture','HIGH_SCHOOL',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",Long.class);
        var access=new LearningAccess(jdbc);ReflectionTestUtils.setField(access,"bootstrapAdmins","");var controller=new LearningController(jdbc,access);
        long course=jdbc.queryForObject("INSERT INTO learning_courses(teacher_id,title,category,status) VALUES (?,'Synthetic query budget','Test','PUBLISHED') RETURNING id",Long.class,teacher);
        jdbc.update("INSERT INTO learning_subjects(course_id,title,sort_order) SELECT ?,'Subject '||g,g FROM generate_series(1,20) g",course);
        jdbc.update("INSERT INTO learning_modules(subject_id,title,sort_order) SELECT s.id,'Module '||g,g FROM learning_subjects s CROSS JOIN generate_series(1,10) g WHERE s.course_id=?",course);
        jdbc.update("INSERT INTO learning_lessons(module_id,title,status,media_key,duration_seconds,validation_status) SELECT m.id,'Lesson '||g,'PUBLISHED',gen_random_uuid(),180,'READY' FROM learning_modules m JOIN learning_subjects s ON s.id=m.subject_id CROSS JOIN generate_series(1,5) g WHERE s.course_id=?",course);
        jdbc.update("INSERT INTO learning_lessons(module_id,title) SELECT m.id,'Private draft' FROM learning_modules m JOIN learning_subjects s ON s.id=m.subject_id WHERE s.course_id=?",course);
        var sample=controller.course("query@example.test",course,false);
        List<Map<String,Object>> subjects=(List<Map<String,Object>>)sample.get("subjects");
        assertEquals(20,subjects.size());assertEquals(10,((List<?>)subjects.get(0).get("modules")).size());
        var module=(Map<?,?>)((List<?>)subjects.get(0).get("modules")).get(0);assertEquals(5,((List<?>)module.get("lessons")).size());
        long[] before=new long[5],after=new long[5];int oldQueries=0,newQueries=0;
        for(int round=0;round<5;round++) {
            jdbc.structures=0;long started=System.nanoTime();oldHierarchy(course);before[round]=(System.nanoTime()-started)/1000;oldQueries=jdbc.structures;
            jdbc.structures=0;started=System.nanoTime();controller.course("query@example.test",course,false);after[round]=(System.nanoTime()-started)/1000;newQueries=jdbc.structures;
        }
        // New queryForList count includes roles (account lookup) plus three structure queries.
        // Roles uses the typed overload and therefore is not counted by this probe.
        assertEquals(221,oldQueries);assertEquals(3,newQueries);
        Arrays.sort(before);Arrays.sort(after);
        Path directory=Path.of(System.getProperty("audit.results","target/audit"));Files.createDirectories(directory);
        Files.writeString(directory.resolve("course-query-budget.json"),"{\"subjects\":20,\"modules\":200,\"publishedLessons\":1000,\"draftLessons\":200,\"oldStructureQueries\":"+oldQueries+",\"newStructureQueries\":"+newQueries+",\"oldMedianMicroseconds\":"+before[2]+",\"newMedianMicroseconds\":"+after[2]+",\"rounds\":5,\"scope\":\"local PostgreSQL, DriverManagerDataSource, not production latency\"}");
        assertEquals(1,controller.courses("query@example.test",false,1,0,"Synthetic").size());
        assertTrue(controller.courses("query@example.test",false,1,1,"Synthetic").isEmpty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.courses("query@example.test",false,201,0,null));
    }
    void oldHierarchy(long course) {for(var subject:jdbc.queryForList("SELECT * FROM learning_subjects WHERE course_id=? ORDER BY sort_order,id",course)) for(var module:jdbc.queryForList("SELECT * FROM learning_modules WHERE subject_id=? ORDER BY sort_order,id",subject.get("id")))jdbc.queryForList("SELECT id,module_id,title,description,objectives,sort_order,status,duration_seconds,validation_status FROM learning_lessons WHERE module_id=? AND status='PUBLISHED' AND validation_status='READY' ORDER BY sort_order,id",module.get("id"));}
    static String env(String name,String fallback){return System.getenv().getOrDefault(name,fallback);}
}
