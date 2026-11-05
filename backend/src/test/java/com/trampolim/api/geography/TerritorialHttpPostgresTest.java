package com.trampolim.api.geography;
import com.trampolim.api.radar.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;import java.net.http.*;import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.trampolim.api.BackendApplication;
import com.trampolim.api.security.TrailTokenService;
import static com.trampolim.api.security.TrailTokenService.Trail.*;
class TerritorialHttpPostgresTest {
 final HttpClient client=HttpClient.newHttpClient();final ObjectMapper json=new ObjectMapper();String base;
 @Test void boundedMunicipalFeedsPreservePublicationPermissionsAndLegacyData()throws Exception {
  String schema="radar_http_"+UUID.randomUUID().toString().replace("-","");
  String[] urls={"jdbc:postgresql://localhost:5432/trampolim_db","jdbc:postgresql://localhost:5433/trampolim_empresas_db","jdbc:postgresql://localhost:5434/trampolim_capacita_db"};String[] users={"root","trampolim_empresas","trampolim_capacita"},passwords={"root","dev_empresas_only","dev_capacita_only"};var admins=new ArrayList<JdbcTemplate>();
  for(int i=0;i<3;i++){var jdbc=new JdbcTemplate(new DriverManagerDataSource(urls[i],users[i],passwords[i]));jdbc.execute("CREATE SCHEMA "+schema);admins.add(jdbc);urls[i]+="?currentSchema="+schema;}
  try(var context=new SpringApplicationBuilder(BackendApplication.class).run("--server.port=0","--spring.jpa.hibernate.ddl-auto=validate","--app.mail.enabled=false","--operations.catalog-jobs-enabled=false","--app.admin.emails=candidate@radar.test","--capacita.admin-emails=teacher@radar.test","--radar.bridge-secret=synthetic-radar-test-secret-not-for-production","--spring.datasource.url="+urls[0],"--spring.datasource.username="+users[0],"--spring.datasource.password="+passwords[0],"--trampolim.datasource.empresas.url="+urls[1],"--trampolim.datasource.empresas.username="+users[1],"--trampolim.datasource.empresas.password="+passwords[1],"--trampolim.datasource.capacita.url="+urls[2],"--trampolim.datasource.capacita.username="+users[2],"--trampolim.datasource.capacita.password="+passwords[2])){
   base="http://127.0.0.1:"+context.getEnvironment().getRequiredProperty("local.server.port")+"/api";var bridge=context.getBean(RadarBridge.class);ReflectionTestUtils.setField(bridge,"base",base);
   var opp=context.getBean("jdbcTemplate",JdbcTemplate.class);var emp=context.getBean("empresasJdbcTemplate",JdbcTemplate.class);var cap=context.getBean("capacitaJdbcTemplate",JdbcTemplate.class);var tokens=context.getBean(TrailTokenService.class);
   long uid=opp.queryForObject("INSERT INTO app_users(email,password_hash,full_name,email_verified) VALUES ('candidate@radar.test','synthetic','Private candidate',TRUE) RETURNING id",Long.class);long otherId=opp.queryForObject("INSERT INTO app_users(email,password_hash,full_name,email_verified) VALUES ('other@radar.test','synthetic','Other',TRUE) RETURNING id",Long.class);
   String candidate=tokens.issue(OPORTUNIDADES,"candidate@radar.test",uid,0),other=tokens.issue(OPORTUNIDADES,"other@radar.test",otherId,0);
   String teacher=registerCap("teacher@radar.test"),student=registerCap("student@radar.test"),stranger=registerCap("stranger@radar.test");
   long ownerId=emp.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status) VALUES ('owner@radar.test','synthetic','Owner',TRUE,'ACTIVE') RETURNING id",Long.class);String owner=tokens.issue(EMPRESAS,"owner@radar.test",ownerId,0);
   long company=emp.queryForObject("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state,verification_status) VALUES (?,'Fixture','Synthetic company','11222333000181','000','Test','Fixture','Teste','SP','APPROVED') RETURNING id",Long.class,ownerId);emp.update("INSERT INTO company_members(company_id,user_id,role) VALUES (?,?,'OWNER')",company,ownerId);
   long job=emp.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,publication_status,is_internship,payment_status) VALUES (?,'job','Synthetic SQL position','Tecnologia','Fixture','SQL with assessment','Teste','Remoto','https://example.test','private-contact@radar.test','PUBLISHED',TRUE,'PAID') RETURNING id",Long.class,company);


   var settings=ok("GET","/public/geography/settings",null,null);assertEquals("irece-ba",settings.path("defaultRegion").asText());assertEquals("2914604",ok("GET","/public/geography/municipalities?q=irece&state=BA",null,null).get(0).path("code").asText());
   assertEquals(0,ok("GET","/public/companies/regional-opportunities?q=no-matching-fixture",null,null).path("items").size());
   assertEquals("irece ba",emp.queryForObject("SELECT geo_normalize('Irecê/BA')",String.class));
   emp.update("UPDATE company_listings SET location='Irece/BA',modality='Presencial' WHERE id=?",job);
   long outside=emp.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,publication_status,is_internship,payment_status) VALUES (?,'job','Outside fixture','Test','Synthetic','Test','Salvador/BA','Presencial','https://example.test','fixture@example.test','PUBLISHED',FALSE,'NOT_REQUIRED') RETURNING id",Long.class,company);
   emp.update("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,publication_status,is_internship,payment_status) VALUES (?,'job','Unknown fixture','Test','Synthetic','Test','Irecê ou região a combinar','Presencial','https://example.test','fixture@example.test','PUBLISHED',FALSE,'NOT_REQUIRED')",company);
   var local=ok("GET","/public/companies/regional-opportunities?region=irece-ba&includeRemote=false",null,null);assertEquals(1,local.path("items").size());assertEquals(job,local.path("items").get(0).path("id").asLong());assertEquals(3,ok("GET","/public/companies/regional-opportunities",null,null).path("items").size());assertEquals(3,ok("GET","/public/companies/regional-opportunities?q=Synthetic%20company",null,null).path("items").size());
   assertEquals(1,ok("GET","/public/companies/regional-opportunities?city=irece&includeRemote=false&region=irece-ba",null,null).path("items").size());
   var first=ok("GET","/public/companies/regional-opportunities?limit=1",null,null);assertFalse(first.path("nextCursor").isNull());var second=ok("GET","/public/companies/regional-opportunities?limit=1&before="+first.path("nextCursor").asText(),null,null);assertNotEquals(first.path("items").get(0).path("id").asLong(),second.path("items").get(0).path("id").asLong());
   // Combined optional filters execute before the cursor, never infer private facts from prose.
   emp.update("UPDATE company_listings SET salary='R$ 1.500,00 a R$ 2.000,00',contract_type='Jovem Aprendiz',area='Agronegócio' WHERE id=?",job);
   emp.update("INSERT INTO company_radar_requirements(listing_id,definition) VALUES (?,?::jsonb)",job,"{\"educationLevel\":2,\"experienceMonths\":0}");
   assertEquals(2,ok("GET","/public/companies/regional-opportunities?state=BA&includeRemote=false",null,null).path("items").size());
   assertEquals(0,ok("GET","/public/companies/regional-opportunities?state=SP&includeRemote=false",null,null).path("items").size());
   String refined="/public/companies/regional-opportunities?area=agribusiness&contractType=Aprendiz&educationLevel=2&maxExperienceMonths=0&minimumSalary=1700&maximumSalary=1900&publishedDays=7&limit=1";
   assertEquals(job,ok("GET",refined,null,null).path("items").get(0).path("id").asLong());
   assertTrue(ok("GET",refined,null,null).path("nextCursor").isNull());
   assertEquals(0,ok("GET",refined+"&before="+job,null,null).path("items").size());
   assertEquals(0,ok("GET","/public/companies/regional-opportunities?minimumSalary=2100",null,null).path("items").size());
   assertEquals(0,ok("GET","/public/companies/regional-opportunities?educationLevel=1",null,null).path("items").size());
   assertEquals(400,send("GET","/public/companies/regional-opportunities?minimumSalary=3000&maximumSalary=1000",null,null).statusCode());
   assertEquals(400,send("GET","/public/companies/regional-opportunities?state=ZZ",null,null).statusCode());
   long course=emp.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,publication_status,payment_status) VALUES (?,'course','Synthetic course','Educação','Fixture','Fixture','Irecê/BA','Online','https://example.test','private@example.test','PUBLISHED','NOT_REQUIRED') RETURNING id",Long.class,company);
   assertEquals(course,ok("GET","/public/companies/regional-opportunities?kind=course&area=education&state=BA",null,null).path("items").get(0).path("id").asLong());
   assertEquals(400,send("GET","/public/companies/regional-opportunities?kind=invalid",null,null).statusCode());
   assertEquals(400,send("GET","/public/companies/regional-opportunities?region=not-configured",null,null).statusCode());
   assertEquals(403,send("PUT","/company/listings/"+job+"/municipality",Map.of("municipalityCode","2914604"),other).statusCode());
   assertEquals(403,send("PUT","/company/listings/"+job+"/municipality",Map.of("municipalityCode","2914604"),student).statusCode());
   assertEquals(400,send("PUT","/company/listings/"+job+"/municipality",Map.of("municipalityCode","9999999"),owner).statusCode());
   assertEquals(204,send("PUT","/company/listings/"+outside+"/municipality",Map.of("municipalityCode","2914604"),owner).statusCode());assertEquals("Irecê — BA",ok("GET","/public/companies/regional-opportunities?municipalityCode=2914604&includeRemote=false",null,null).path("items").get(0).path("municipalityLabel").asText());assertEquals("Salvador/BA",emp.queryForObject("SELECT location FROM company_listings WHERE id=?",String.class,outside));assertEquals(2,ok("GET","/public/companies/regional-opportunities?municipalityCode=2914604&includeRemote=false",null,null).path("items").size());
   emp.update("UPDATE company_listings SET payment_status='PENDING' WHERE id=?",job);assertEquals(1,ok("GET","/public/companies/regional-opportunities?municipalityCode=2914604&includeRemote=false",null,null).path("items").size());emp.update("UPDATE company_listings SET expires_at=CURRENT_DATE-1 WHERE id=?",outside);assertEquals(0,ok("GET","/public/companies/regional-opportunities?municipalityCode=2914604&includeRemote=false",null,null).path("items").size());
   long catalog=opp.queryForObject("INSERT INTO catalog_items(kind,title,provider,description,url,area,location,modality) VALUES ('job','Synthetic catalog','Fixture','Fixture','https://example.test','admin','Irecê - BA','Presencial') RETURNING id",Long.class);
   var catalogPage=ok("GET","/opportunities/job/regional?region=irece-ba&includeRemote=false&city=irece",null,candidate);assertEquals(1,catalogPage.path("items").size());assertTrue(catalogPage.path("items").get(0).has("createdAt"));assertEquals(403,send("GET","/opportunities/job/regional",null,student).statusCode());assertEquals(403,send("PUT","/admin/catalog/"+catalog+"/municipality",Map.of("municipalityCode","2914604"),other).statusCode());assertEquals(204,send("PUT","/admin/catalog/"+catalog+"/municipality",Map.of("municipalityCode","2914604"),candidate).statusCode());assertEquals("Irecê - BA",opp.queryForObject("SELECT location FROM catalog_items WHERE id=?",String.class,catalog));
   opp.update("UPDATE catalog_items SET salary='2500',contract_type='CLT',experience_level='Sem experiência',published_at=CURRENT_TIMESTAMP WHERE id=?",catalog);
   assertEquals(1,ok("GET","/opportunities/job/regional?state=BA&includeRemote=false&contractType=CLT&minimumSalary=2000&publishedDays=1",null,candidate).path("items").size());
   assertEquals(0,ok("GET","/opportunities/job/regional?minimumSalary=3000",null,candidate).path("items").size());
   assertEquals(0,ok("GET","/opportunities/job/regional?educationLevel=2",null,candidate).path("items").size());
   opp.update("UPDATE catalog_items SET latitude=-11.30,longitude=-41.85 WHERE id=?",catalog);
   assertEquals(1,ok("GET","/opportunities/job/regional?radiusKm=5&latitude=-11.30&longitude=-41.85",null,candidate).path("items").size());
   assertEquals(0,ok("GET","/opportunities/job/regional?radiusKm=5&latitude=-12&longitude=-41",null,candidate).path("items").size());
   assertEquals(400,send("GET","/opportunities/job/regional?radiusKm=5&latitude=NaN&longitude=-41",null,candidate).statusCode());
   assertFalse(catalogPage.toString().contains("password_hash"));assertEquals(0,cap.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema=? AND column_name='municipality_code'",Integer.class,schema));
  }finally{for(var admin:admins)admin.execute("DROP SCHEMA "+schema+" CASCADE");}
 }
 String registerCap(String email)throws Exception{return ok("POST","/capacita/auth/register",Map.of("name","Synthetic radar student","email",email,"password","RadarSyntheticPassword!","educationStage","HIGH_SCHOOL","ageBand","AGE_18_PLUS","termsAccepted",true),null).path("token").asText();}
 JsonNode ok(String method,String path,Object body,String token)throws Exception{var response=send(method,path,body,token);assertEquals(200,response.statusCode(),response.body());return json.readTree(response.body());}
 HttpResponse<String> send(String method,String path,Object body,String token)throws Exception{var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(java.time.Duration.ofSeconds(30)).header("Content-Type","application/json");if(token!=null)request.header("Authorization","Bearer "+token);return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
}
