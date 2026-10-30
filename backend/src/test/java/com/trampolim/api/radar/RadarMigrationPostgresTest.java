package com.trampolim.api.radar;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
class RadarMigrationPostgresTest {
 @Test void upgradesAllThreeExistingSchemasWithoutReplacingUsers(){
  String[] urls={"jdbc:postgresql://localhost:5432/trampolim_db","jdbc:postgresql://localhost:5433/trampolim_empresas_db","jdbc:postgresql://localhost:5434/trampolim_capacita_db"},users={"root","trampolim_empresas","trampolim_capacita"},passwords={"root","dev_empresas_only","dev_capacita_only"},locations={"classpath:db/migration","classpath:db/migration-empresas","classpath:db/migration-capacita"},tables={"app_users","company_users","student_users"};int[] old={13,10,15},latest={17,14,19};
  for(int i=0;i<3;i++){String schema="radar_upgrade_"+UUID.randomUUID().toString().replace("-","");var admin=new JdbcTemplate(new DriverManagerDataSource(urls[i],users[i],passwords[i]));admin.execute("CREATE SCHEMA "+schema);try{var source=new DriverManagerDataSource(urls[i]+"?currentSchema="+schema,users[i],passwords[i]);Flyway.configure().dataSource(source).locations(locations[i]).target(Integer.toString(old[i])).load().migrate();var jdbc=new JdbcTemplate(source);
    String extra=i==2?",education_stage,terms_accepted_at":"",value=i==2?",'HIGH_SCHOOL',CURRENT_TIMESTAMP":"";long id=jdbc.queryForObject("INSERT INTO "+tables[i]+"(email,password_hash,full_name"+extra+") VALUES ('fixture@upgrade.test','preserved-hash','Preserved user'"+value+") RETURNING id",Long.class);
    var migration=Flyway.configure().dataSource(source).locations(locations[i]).load();assertEquals(latest[i]-old[i],migration.migrate().migrationsExecuted);assertEquals(0,migration.migrate().migrationsExecuted);assertEquals("preserved-hash",jdbc.queryForObject("SELECT password_hash FROM "+tables[i]+" WHERE id=?",String.class,id));assertEquals(latest[i],Integer.parseInt(migration.info().current().getVersion().toString()));
    if(i==0){assertEquals(13,jdbc.queryForObject("SELECT count(*) FROM skill_concepts",Integer.class));assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO skill_terms(term,skill_code,display_name) VALUES ('sql','java','Duplicate')"));}
   }finally{admin.execute("DROP SCHEMA "+schema+" CASCADE");}}
 }
}
