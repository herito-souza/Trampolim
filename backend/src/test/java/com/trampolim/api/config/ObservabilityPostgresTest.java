package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import io.micrometer.core.instrument.MeterRegistry;
import com.trampolim.api.BackendApplication;
@SpringBootTest(classes=BackendApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"management.server.port=0","operations.monitoring-key=observability-test-secret-at-least-32-characters"})
class ObservabilityPostgresTest {
 @Autowired java.util.List<com.trampolim.api.communication.CommunicationStore> stores;
 @Autowired Environment env; @Autowired MeterRegistry metrics; @Autowired OperationalDatabaseMetrics databases;
 private HttpResponse<String> get(String path,String key)throws Exception{var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+env.getProperty("local.management.port")+path));if(key!=null)request.header("X-Monitoring-Key",key);return HttpClient.newHttpClient().send(request.GET().build(),HttpResponse.BodyHandlers.ofString());}
 @Test void realManagementPortProtectsScrapeAndExportsThreePools()throws Exception{
  assertEquals(403,get("/actuator/prometheus",null).statusCode());
  databases.sample();var response=get("/actuator/prometheus","observability-test-secret-at-least-32-characters");assertEquals(200,response.statusCode());assertTrue(response.body().contains("trampolim_database_up"));assertTrue(response.body().contains("hikaricp_connections"));
  for(String pool:new String[]{"oportunidades","empresas","capacita"})assertTrue(response.body().contains("pool=\""+pool+"\""));
  for(String trail:new String[]{"oportunidades","empresas","capacita"})assertEquals(1,metrics.get("trampolim.database.up").tag("trail",trail).gauge().value());
 }
 @Test void readinessAndLivenessArePublicWithoutPrivateDetails()throws Exception{for(String path:new String[]{"/actuator/health/readiness","/actuator/health/liveness"}){var response=get(path,null);assertEquals(200,response.statusCode());assertTrue(response.body().contains("UP"));assertFalse(response.body().contains("jdbc:"));assertFalse(response.body().contains("components"));}}
 @Test void realQueueAggregatesRunIndependentlyOnEachDatabase(){assertEquals(3,stores.size());for(var store:stores){var snapshot=store.queueSnapshot();assertTrue(snapshot.pending()>=0);assertTrue(snapshot.failed()>=0);assertTrue(snapshot.oldestDueSeconds()>=0);}}
 @Test void sensitiveActuatorEndpointsRemainUnavailable()throws Exception{assertNotEquals(200,get("/actuator/env",null).statusCode());}
}
