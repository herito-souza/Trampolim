package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FeedIndexMigrationPostgresTest {
    @Test void upgradePreservesRowsAndUsesPublishedFeedIndex() throws Exception {
        String url=System.getenv().getOrDefault("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db"), user=System.getenv().getOrDefault("CAPACITA_DB_USERNAME","trampolim_capacita"),password=System.getenv().getOrDefault("CAPACITA_DB_PASSWORD","dev_capacita_only");
        var admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));String schema="index_audit_"+UUID.randomUUID().toString().replace("-","");admin.execute("CREATE SCHEMA "+schema);
        try {
            var source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);
            Flyway.configure().dataSource(source).locations("classpath:db/migration-capacita").target("14").load().migrate();var jdbc=new JdbcTemplate(source);
            long teacher=jdbc.queryForObject("INSERT INTO student_users(email,password_hash,full_name,education_stage,email_verified,account_status,terms_accepted_at) VALUES ('index@example.test','synthetic','Fixture','HIGH_SCHOOL',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",Long.class);
            jdbc.update("INSERT INTO learning_courses(teacher_id,title,category,status) SELECT ?,'Synthetic '||g,'Test',CASE WHEN g%2=0 THEN 'PUBLISHED' ELSE 'DRAFT' END FROM generate_series(1,20000) g",teacher);jdbc.execute("ANALYZE learning_courses");
            String query="EXPLAIN (ANALYZE,BUFFERS) SELECT id,title,updated_at FROM learning_courses WHERE status='PUBLISHED' AND moderation_blocked=FALSE ORDER BY updated_at DESC,id DESC LIMIT 25";
            String before=String.join("\n",jdbc.queryForList(query,String.class));
            Flyway.configure().dataSource(source).locations("classpath:db/migration-capacita").load().migrate();jdbc.execute("ANALYZE learning_courses");
            String after=String.join("\n",jdbc.queryForList(query,String.class));assertTrue(after.contains("idx_learning_courses_public_feed"));assertEquals(20000,jdbc.queryForObject("SELECT count(*) FROM learning_courses",Integer.class));
            Path dir=Path.of(System.getProperty("audit.results","target/audit"));Files.createDirectories(dir);Files.writeString(dir.resolve("feed-explain-analyze.txt"),"Synthetic courses: 20,000 (10,000 published)\nBEFORE V15\n"+before+"\nAFTER V15\n"+after+"\n");
        } finally {admin.execute("DROP SCHEMA "+schema+" CASCADE");}
    }
}
