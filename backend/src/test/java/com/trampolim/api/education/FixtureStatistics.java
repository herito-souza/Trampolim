package com.trampolim.api.education;
import org.springframework.jdbc.core.JdbcTemplate;
/** Never issue database-wide ANALYZE from a synthetic performance harness. */
final class FixtureStatistics {
 static void analyze(JdbcTemplate jdbc){
  String schema=jdbc.queryForObject("SELECT current_schema()",String.class);
  if(schema==null||!schema.matches("perf_[0-9a-f]{32}"))throw new IllegalStateException("Statistics are restricted to a disposable performance schema");
  for(String table:jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname=?",String.class,schema)){
   if(!table.matches("[a-z_][a-z0-9_]*"))throw new IllegalStateException("Unexpected fixture table identifier");
   jdbc.execute("ANALYZE "+schema+"."+table);
  }
 }
}
