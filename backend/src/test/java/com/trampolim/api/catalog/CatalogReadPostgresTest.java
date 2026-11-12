package com.trampolim.api.catalog;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.BackendApplication;
/** Small, always-on PostgreSQL regression; all writes go to a disposable schema. */
class CatalogReadPostgresTest {
 @Test void sqlFiltersBeforePaginationAndChangesRemainVisible(){
  String url=System.getenv().getOrDefault("OPORTUNIDADES_DB_URL","jdbc:postgresql://localhost:5432/trampolim_db");
  if(!url.matches("jdbc:postgresql://(localhost|127[.]0[.]0[.]1):[0-9]+/[a-zA-Z0-9_]+"))throw new IllegalStateException("Local test database required");
  String user=System.getenv().getOrDefault("OPORTUNIDADES_DB_USERNAME","root"),password=System.getenv().getOrDefault("OPORTUNIDADES_DB_PASSWORD","root"),schema="catalog_read_"+UUID.randomUUID().toString().replace("-","");
  var admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));admin.execute("CREATE SCHEMA "+schema);
  try(var context=new SpringApplicationBuilder(BackendApplication.class).run("--server.port=0","--management.server.port=0","--spring.profiles.active=test","--spring.datasource.url="+url+"?currentSchema="+schema,"--spring.datasource.username="+user,"--spring.datasource.password="+password,"--spring.jpa.hibernate.ddl-auto=validate","--trampolim.datasource.empresas.enabled=false","--trampolim.datasource.capacita.enabled=false","--app.mail.enabled=false","--operations.catalog-jobs-enabled=false","--future.poll-enabled=false","--spring.jpa.properties.hibernate.session_factory.statement_inspector="+SessionProjectionInspector.class.getName())){
   var repository=context.getBean(CatalogItemRepository.class);var controller=context.getBean(CatalogController.class);var today=LocalDate.now(ZoneId.of("America/Sao_Paulo"));var fixture=new ArrayList<CatalogItem>();
   for(int i=0;i<20;i++){var item=new CatalogItem();item.setKind("job");item.setTitle("Synthetic listing "+i);item.setProvider("Synthetic");item.setDescription("Isolated regression fixture");item.setUrl("https://example.test/"+i);item.setArea("admin");item.setActive(i!=2);item.setModerationStatus(i==1?"PENDING":"APPROVED");item.setExpiresAt(i==0?today.minusDays(1):today.plusDays(10));item.setCreatedAt(Instant.now().minusSeconds(i));fixture.add(item);}
   fixture= new ArrayList<>(repository.saveAllAndFlush(fixture));
   var first=controller.list("job",3,0);var second=controller.list("job",3,1);assertEquals(3,first.size());assertEquals(3,second.size());assertEquals(17,controller.list("job").size());
   var forbidden=Set.of(fixture.get(0).getId(),fixture.get(1).getId(),fixture.get(2).getId());assertTrue(first.stream().noneMatch(item->forbidden.contains(item.id())));assertTrue(second.stream().noneMatch(item->first.stream().anyMatch(previous->previous.id().equals(item.id()))));
   long withdrawn=first.get(0).id();var item=repository.findById(withdrawn).orElseThrow();item.setActive(false);repository.saveAndFlush(item);assertTrue(controller.list("job",3,0).stream().noneMatch(row->row.id().equals(withdrawn)));
   var jdbc=context.getBean("jdbcTemplate",JdbcTemplate.class);var geo=context.getBean(com.trampolim.api.geography.TerritorialCatalog.class);
   String[] titles={"Ação São","Ex-cel","écoleAção249","Raíz ecológica","Weiß MÃOS","SQL-249"};
   for(String title:titles){var sample=new CatalogItem();sample.setKind("job");sample.setTitle(title);sample.setDescription("Paulo, conteúdo de teste");sample.setProvider("Instituição sintética");sample.setArea("admin");sample.setUrl("https://example.test/search");sample.setActive(true);sample.setModerationStatus("APPROVED");repository.saveAndFlush(sample);}
   jdbc.update("INSERT INTO app_users(email,password_hash,full_name,resume_content,photo_content,token_version) VALUES ('session@projection.test','synthetic','Synthetic session',decode(repeat('ab',1048576),'hex'),decode(repeat('cd',262144),'hex'),3)");
   var users=context.getBean(com.trampolim.api.user.AppUserRepository.class);var full=users.findByEmail("session@projection.test").orElseThrow();assertEquals(1048576,full.getResumeContent().length);assertEquals(262144,full.getPhotoContent().length);assertTrue(SessionProjectionInspector.last.contains("resume_content"));
   SessionProjectionInspector.last="";var session=users.findSessionByEmail("session@projection.test").orElseThrow();assertEquals(3,session.tokenVersion());assertFalse(SessionProjectionInspector.last.contains("resume_content"));assertFalse(SessionProjectionInspector.last.contains("photo_content"));assertFalse(SessionProjectionInspector.last.contains("password_hash"));
   jdbc.update("UPDATE app_users SET token_version=4 WHERE email='session@projection.test'");assertEquals(4,users.findSessionByEmail("session@projection.test").orElseThrow().tokenVersion());
   for(String term:List.of("ação","sao paulo","249","SQL","Excel","Ex-cel","ecole","raiz","MÃOS","instituição","%","A&B")){
    String columns="concat_ws(' ',l.title,l.description,l.location,l.area,l.provider)";
    var expected=jdbc.queryForList("SELECT l.id FROM catalog_items l WHERE geo_normalize("+columns+") LIKE ? ORDER BY l.id",Long.class,"%"+com.trampolim.api.geography.TerritorialCatalog.normalize(term)+"%");
    var filter=com.trampolim.api.geography.RegionalQuery.filter(geo,new com.trampolim.api.geography.RegionalQuery.Filters("","",false,term,"","","",Long.MAX_VALUE,50),"l");
    assertEquals(expected,jdbc.queryForList("SELECT l.id FROM catalog_items l WHERE TRUE"+filter.where()+" ORDER BY l.id",Long.class,filter.args().toArray()),"Search semantics changed for "+term);
   }
   assertEquals(400,assertThrows(ResponseStatusException.class,()->controller.list("job",201,0)).getStatusCode().value());assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.adminList("ordinary@example.test","job",10,0)).getStatusCode().value());
  }finally{admin.execute("DROP SCHEMA "+schema+" CASCADE");}
 }
}
