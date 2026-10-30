package com.trampolim.api.future;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import com.trampolim.api.radar.*;
class FutureQueryBudgetPostgresTest {
 @Test void upgradePreservesRadarAndAggregatesTenThousandSyntheticInterestedCandidates()throws Exception{
  String url="jdbc:postgresql://localhost:5432/trampolim_db",schema="future_budget_"+UUID.randomUUID().toString().replace("-","");var admin=new JdbcTemplate(new DriverManagerDataSource(url,"root","root"));admin.execute("CREATE SCHEMA "+schema);
  try{var source=new DriverManagerDataSource(url+"?currentSchema="+schema,"root","root");Flyway.configure().dataSource(source).locations("classpath:db/migration").target("14").load().migrate();var jdbc=new JdbcTemplate(source);jdbc.update("INSERT INTO app_users(email,password_hash,full_name,email_verified) SELECT 'synthetic'||g||'@budget.test','preserved-hash','Synthetic fixture',TRUE FROM generate_series(1,10000) g");jdbc.update("INSERT INTO candidate_skill_declarations(user_id,skill_code,level) SELECT id,'sql',2 FROM app_users");Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();assertEquals(10000,jdbc.queryForObject("SELECT count(*) FROM candidate_skill_declarations WHERE level=2",Integer.class));
   jdbc.update("INSERT INTO future_talent_interests(user_id,demand_id,occupation,area,notify) SELECT id,123,'Synthetic SQL','Test',id%3=0 FROM app_users");jdbc.update("INSERT INTO future_preparation_snapshots(interest_id,requirements_revision,policy_revision,score,ready,baseline_gaps,gaps,met,verified_met) SELECT id,1,1,0,FALSE,'[\"sql\"]','[\"sql\"]','[]','[]' FROM future_talent_interests");jdbc.execute("ANALYZE future_talent_interests");jdbc.execute("ANALYZE future_preparation_snapshots");
   var bridge=new RadarBridge();ReflectionTestUtils.setField(bridge,"secret","synthetic-budget-key-not-for-production");var radar=mock(RadarService.class);when(radar.policy()).thenReturn(new RadarModel.Policy(1,Map.of()));var service=new FutureTalentService(jdbc,bridge,null,radar,new JdbcTransactionManager(source));long started=System.nanoTime();var result=service.summary("synthetic-budget-key-not-for-production",new FutureModel.SummaryRequest(123,1));long elapsed=(System.nanoTime()-started)/1000000;assertEquals("10000–10004",((Map<?,?>)result.get("interested")).get("label"));assertEquals(1,((List<?>)result.get("gaps")).size());String plan=String.join("\n",jdbc.queryForList("EXPLAIN (ANALYZE,BUFFERS) SELECT user_id FROM future_talent_interests WHERE demand_id=123 AND user_id>0 AND notify=TRUE ORDER BY user_id LIMIT 500",String.class));assertTrue(plan.contains("idx_future_interest_notify"),plan);assertFalse(result.toString().contains("@budget.test"));assertFalse(result.toString().contains("user_id"));Path output=Path.of("target/audit/future-query-plan.txt");Files.createDirectories(output.getParent());Files.writeString(output,"Synthetic fixture: 10000 interested candidates. Aggregate elapsed ms="+elapsed+"\n"+plan);System.out.println("FUTURE_AGGREGATE_BUDGET: 10000 synthetic candidates, aggregate ms="+elapsed+", delivery page capped at 500 using partial index.");
  }finally{admin.execute("DROP SCHEMA "+schema+" CASCADE");}
 }
}
