package com.trampolim.api.company;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class RecruitmentMigrationPostgresTest {
    @Test void upgradesExistingApplicationsWithoutRewritingCandidatesOrInventingTransitions(){
        String url="jdbc:postgresql://localhost:5433/trampolim_empresas_db",user="trampolim_empresas",password="dev_empresas_only";
        String schema="recruitment_upgrade_"+UUID.randomUUID().toString().replace("-","");
        var admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));admin.execute("CREATE SCHEMA "+schema);
        try{
            var source=new DriverManagerDataSource(url+"?currentSchema="+schema,user,password);var jdbc=new JdbcTemplate(source);
            Flyway.configure().dataSource(source).locations("classpath:db/migration-empresas").target("16").load().migrate();
            long uid=jdbc.queryForObject("INSERT INTO company_users(email,password_hash,full_name) VALUES ('migration@example.test','preserved-hash','Pessoa de teste') RETURNING id",Long.class);
            long company=jdbc.queryForObject("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state) VALUES (?,'Migração sintética','Migração sintética','00000000000001','fixture','fixture','fixture','Irecê','BA') RETURNING id",Long.class,uid);
            long listing=jdbc.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email) VALUES (?,'job','Vaga de teste','fixture','fixture','fixture','Irecê','Presencial','https://example.test','fixture@example.test') RETURNING id",Long.class,company);
            for(String state:java.util.List.of("RECEIVED","IN_REVIEW","CONTACTED","ACCEPTED","DECLINED"))jdbc.update("INSERT INTO company_applications(listing_id,candidate_name,candidate_email,status,created_at) VALUES (?,'Pessoa preservada',?,?,CURRENT_TIMESTAMP-INTERVAL '10 days')",listing,state+"@example.test",state);
            var before=jdbc.queryForList("SELECT id,candidate_name,candidate_email,status,created_at FROM company_applications ORDER BY id");
            var flyway=Flyway.configure().dataSource(source).locations("classpath:db/migration-empresas").load();assertEquals(5,flyway.migrate().migrationsExecuted);assertEquals(0,flyway.migrate().migrationsExecuted);
            assertEquals(before,jdbc.queryForList("SELECT id,candidate_name,candidate_email,status,created_at FROM company_applications ORDER BY id"));
            assertEquals(5,jdbc.queryForObject("SELECT count(*) FROM company_recruitment_events WHERE event_type='BASELINE' AND revision=0 AND previous_status IS NULL AND actor_user_id IS NULL",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM company_recruitment_events WHERE created_at<CURRENT_TIMESTAMP-INTERVAL '1 day'",Integer.class));
            jdbc.update("INSERT INTO company_applications(listing_id,candidate_name,candidate_email) VALUES (?,'Nova pessoa sintética','new@example.test')",listing);
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM company_recruitment_events WHERE event_type='RECEIVED'",Integer.class));
            assertEquals("preserved-hash",jdbc.queryForObject("SELECT password_hash FROM company_users WHERE id=?",String.class,uid));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM company_recruitment_notes",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM communication_events",Integer.class));
        }finally{if(!schema.matches("recruitment_upgrade_[0-9a-f]{32}"))throw new IllegalStateException("Unexpected test schema");admin.execute("DROP SCHEMA "+schema+" CASCADE");}
    }
}
