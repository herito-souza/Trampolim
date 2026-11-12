package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
class PerformanceHarnessIsolationPostgresTest {
 @Test void statisticsCannotEscapeTheSyntheticSchema(){
  String url=System.getenv().getOrDefault("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db"),user=System.getenv().getOrDefault("CAPACITA_DB_USERNAME","trampolim_capacita"),password=System.getenv().getOrDefault("CAPACITA_DB_PASSWORD","dev_capacita_only");
  if(!url.matches("jdbc:postgresql://(localhost|127[.]0[.]0[.]1):[0-9]+/[a-zA-Z0-9_]+"))throw new IllegalStateException("Local test database required");
  String suffix=UUID.randomUUID().toString().replace("-",""),schema="perf_"+suffix,control="perf_control_"+suffix;
  var admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));admin.execute("CREATE SCHEMA "+schema);admin.execute("CREATE SCHEMA "+control);
  try{
   for(String name:java.util.List.of(schema,control)){admin.execute("CREATE TABLE "+name+".probe(value INTEGER) WITH (autovacuum_enabled=false)");admin.update("INSERT INTO "+name+".probe SELECT g FROM generate_series(1,100) g");}
   var jdbc=new JdbcTemplate(new DriverManagerDataSource(url+"?currentSchema="+schema,user,password));FixtureStatistics.analyze(jdbc);
   assertEquals(1,admin.queryForObject("SELECT count(*) FROM pg_stats WHERE schemaname=? AND tablename='probe'",Integer.class,schema));
   assertEquals(0,admin.queryForObject("SELECT count(*) FROM pg_stats WHERE schemaname=? AND tablename='probe'",Integer.class,control));
   assertThrows(IllegalStateException.class,()->FixtureStatistics.analyze(admin));
  }finally{admin.execute("DROP SCHEMA "+schema+" CASCADE");admin.execute("DROP SCHEMA "+control+" CASCADE");}
 }
}
