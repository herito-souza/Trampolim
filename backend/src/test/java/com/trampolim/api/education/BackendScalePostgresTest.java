package com.trampolim.api.education;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.sql.*;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.flywaydb.core.Flyway;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.trampolim.api.BackendApplication;
import com.trampolim.api.security.TrailTokenService;

/** Opt-in local measurement, disposable schemas only. Never a production capacity certification. */
@EnabledIfSystemProperty(named="audit.performance.enabled",matches="true")
class BackendScalePostgresTest {
 private static final ObjectMapper JSON=new ObjectMapper();
 private final Path output=Path.of(System.getProperty("audit.performance.output","target/audit/backend-scale"));
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
 private record Store(String trail,String url,String user,String password,String schema,HikariDataSource pool,JdbcTemplate jdbc,JdbcTemplate admin){}
 private static String env(String name,String fallback){return System.getenv().getOrDefault(name,fallback);}
 private Store store(String trail,int port,String database,String user,String password,String schema){
  String prefix=trail.toUpperCase(Locale.ROOT);String url=env(prefix+"_DB_URL","jdbc:postgresql://localhost:"+port+"/"+database);
  if(!url.matches("jdbc:postgresql://(localhost|127[.]0[.]0[.]1):[0-9]+/[a-zA-Z0-9_]+")||!schema.matches("perf_[0-9a-f]{32}"))throw new IllegalStateException("Only local databases and disposable schemas are allowed");
  user=env(prefix+"_DB_USERNAME",user);password=env(prefix+"_DB_PASSWORD",password);
  var admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));admin.execute("CREATE SCHEMA "+schema);
  var pool=new HikariDataSource();pool.setJdbcUrl(url+"?currentSchema="+schema);pool.setUsername(user);pool.setPassword(password);pool.setMaximumPoolSize(6);pool.setMinimumIdle(1);pool.setConnectionTimeout(2000);pool.setPoolName("synthetic-"+trail);
  var jdbc=new JdbcTemplate(pool);jdbc.setQueryTimeout(15);return new Store(trail,url,user,password,schema,pool,jdbc,admin);
 }
 private void migrate(Store s,String target){var config=Flyway.configure().dataSource(s.pool()).locations("classpath:db/"+(s.trail().equals("oportunidades")?"migration":"migration-"+s.trail())).schemas(s.schema());if(target!=null)config.target(target);config.load().migrate();}
 @Test void comparePlansAndRunBoundedHttpLoadWithoutRealData()throws Exception {
  Files.createDirectories(output);var stores=new ArrayList<Store>();String schema="perf_"+UUID.randomUUID().toString().replace("-","");var evidence=new LinkedHashMap<String,Object>();
  try{
   var opp=store("oportunidades",5432,"trampolim_db","root","root",schema);stores.add(opp);migrate(opp,"18");
   var company=store("empresas",5433,"trampolim_empresas_db","trampolim_empresas","dev_empresas_only",schema);stores.add(company);migrate(company,"15");
   var cap=store("capacita",5434,"trampolim_capacita_db","trampolim_capacita","dev_capacita_only",schema);stores.add(cap);migrate(cap,"19");
   seed(opp.jdbc(),company.jdbc(),cap.jdbc());
   var counted=new Counted(cap.pool());var countedJdbc=new JdbcTemplate(counted);var legacy=new LegacyLearningPerformanceBaseline(countedJdbc);
   var before=measure(()->legacy.dashboard("student2@performance.test"),counted);evidence.put("learningBefore",before);
   String events="SELECT l.id,(SELECT count(*) FROM company_listing_events e WHERE e.listing_id=l.id AND e.event_type='LISTING_VIEW') AS views FROM company_listings l WHERE l.company_id=1 ORDER BY l.created_at DESC,l.id DESC LIMIT 20";
   String progress="SELECT lesson_id FROM learning_lesson_progress WHERE student_id=2 ORDER BY updated_at DESC LIMIT 100";
   String catalog="SELECT id FROM catalog_items WHERE kind='job' AND active=TRUE AND moderation_status='APPROVED' AND (expires_at IS NULL OR expires_at>=CURRENT_DATE) ORDER BY created_at DESC,id DESC LIMIT 20";
   var planBefore=Map.of("company",plan(company.jdbc(),events,"company-before"),"progress",plan(cap.jdbc(),progress,"progress-before"),"catalog",plan(opp.jdbc(),catalog,"catalog-before"));evidence.put("plansBeforeMs",planBefore);
   for(var s:stores)migrate(s,null);
   for(var s:stores)FixtureStatistics.analyze(s.jdbc());
   var access=new LearningAccess(countedJdbc);ReflectionTestUtils.setField(access,"bootstrapAdmins","");var optimized=new LearningPerformanceService(countedJdbc,access,null);
   assertEquals(JSON.readTree(JSON.writeValueAsString(legacy.dashboard("student2@performance.test"))),JSON.readTree(JSON.writeValueAsString(optimized.dashboard("student2@performance.test"))),"Completion criteria and response fields must remain identical");
   var after=measure(()->optimized.dashboard("student2@performance.test"),counted);evidence.put("learningAfter",after);
   assertEquals(8,((Number)before.get("queriesPerCall")).intValue());assertEquals(5,((Number)after.get("queriesPerCall")).intValue());
   assertTrue(((Number)after.get("rowsPerCall")).longValue()<((Number)before.get("rowsPerCall")).longValue());
   evidence.put("plansAfterMs",Map.of("company",plan(company.jdbc(),events,"company-after"),"progress",plan(cap.jdbc(),progress,"progress-after"),"catalog",plan(opp.jdbc(),catalog,"catalog-after")));
   runHttp(stores,evidence);
   evidence.put("fixture",Map.of("catalogItems",50000,"companies",100,"corporateListings",1000,"corporateEvents",100000,"students",500,"courses",50,"lessons",10000,"progressRows",49920));
   evidence.put("environment",Map.of("java",System.getProperty("java.version"),"os",System.getProperty("os.name"),"processors",Runtime.getRuntime().availableProcessors(),"maxHeapBytes",Runtime.getRuntime().maxMemory(),"poolMaxPerTrail",6));
   evidence.put("scope","Local synthetic read workload; JVM and load generator share a machine. No thousands-of-users or production-capacity claim. Soak is short, not a multi-hour endurance test.");
   evidence.put("realPersonalDataUsed",false);evidence.put("schema",schema);
  } finally {
   for(var s:stores){s.pool().close();s.admin().execute("DROP SCHEMA "+s.schema()+" CASCADE");}
   evidence.put("disposableSchemasRemoved",stores.stream().allMatch(s->s.admin().queryForObject("SELECT count(*) FROM information_schema.schemata WHERE schema_name=?",Integer.class,s.schema())==0));
   Files.writeString(output.resolve("metrics.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
  }
 }
 private void seed(JdbcTemplate o,JdbcTemplate c,JdbcTemplate s){
  o.update("INSERT INTO app_users(email,password_hash,full_name,email_verified) VALUES ('candidate@performance.test','synthetic-not-a-login-password','Synthetic candidate',TRUE)");
  o.update("UPDATE app_users SET resume_content=decode(repeat('ab',1048576),'hex'),photo_content=decode(repeat('cd',262144),'hex') WHERE email='candidate@performance.test'");
  o.update("INSERT INTO catalog_items(kind,title,provider,description,url,area,location,municipality_code,modality,active,moderation_status,expires_at,created_at) SELECT 'job','Fixture SQL '||g,'Synthetic provider','Synthetic performance fixture','https://example.test/'||g,'admin','Irecê/BA','2914604',CASE WHEN g%2=0 THEN 'Remoto' ELSE 'Presencial' END,g%7<>0,CASE WHEN g%5=0 THEN 'PENDING' ELSE 'APPROVED' END,CASE WHEN g%11=0 THEN DATE '2000-01-01' ELSE DATE '2099-01-01' END,CURRENT_TIMESTAMP-(g||' seconds')::interval FROM generate_series(1,50000) g");
  c.update("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status) SELECT 'company'||g||'@performance.test','synthetic','Synthetic corporate user',TRUE,'ACTIVE' FROM generate_series(1,100) g");
  c.update("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state,verification_status) SELECT id,'Synthetic legal','Synthetic company '||id,lpad(id::text,14,'0'),'000','Synthetic','Synthetic address','Irecê','BA','APPROVED' FROM company_users");
  c.update("INSERT INTO company_members(company_id,user_id,role) SELECT id,owner_user_id,'OWNER' FROM companies");
  c.update("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,publication_status,payment_status,created_at) SELECT CASE WHEN g<=500 THEN 1 ELSE 2+(g%99) END,'job','Fixture listing '||g,'admin','Synthetic fixture','SQL','Irecê/BA','Presencial','https://example.test/listing','synthetic@example.test','PUBLISHED','NOT_PROCESSED',CURRENT_TIMESTAMP-(g||' seconds')::interval FROM generate_series(1,1000) g");
  c.update("INSERT INTO company_listing_events(listing_id,event_type) SELECT 1+(g%1000),CASE WHEN g%3=0 THEN 'APPLICATION_CLICK' ELSE 'LISTING_VIEW' END FROM generate_series(1,100000) g");
  s.update("INSERT INTO student_users(email,password_hash,full_name,education_stage,email_verified,account_status,terms_accepted_at) SELECT 'student'||g||'@performance.test','synthetic','Synthetic student','HIGH_SCHOOL',TRUE,'ACTIVE',CURRENT_TIMESTAMP FROM generate_series(1,500) g");
  s.update("INSERT INTO learning_account_roles(user_id,role) VALUES (1,'TEACHER')");
  s.update("INSERT INTO learning_courses(teacher_id,title,category,status) SELECT 1,'Fixture course '||g,'Synthetic','PUBLISHED' FROM generate_series(1,50) g");
  s.update("INSERT INTO learning_subjects(course_id,title) SELECT id,'Synthetic subject' FROM learning_courses");
  s.update("INSERT INTO learning_modules(subject_id,title,sort_order) SELECT id,'Synthetic module '||g,g FROM learning_subjects CROSS JOIN generate_series(1,10) g");
  s.update("INSERT INTO learning_lessons(module_id,title,status,media_key,duration_seconds,validation_status,sort_order) SELECT id,'Synthetic lesson '||g,'PUBLISHED',gen_random_uuid(),180,'READY',g FROM learning_modules CROSS JOIN generate_series(1,20) g");
  s.update("INSERT INTO learning_lesson_progress(student_id,lesson_id,completed_at,updated_at) SELECT 2,id,CASE WHEN id%4<>0 THEN CURRENT_TIMESTAMP END,CURRENT_TIMESTAMP-(id||' seconds')::interval FROM learning_lessons");
  s.update("INSERT INTO learning_lesson_progress(student_id,lesson_id) SELECT u.id,l.id FROM student_users u CROSS JOIN (SELECT id FROM learning_lessons ORDER BY id LIMIT 80) l WHERE u.id<>2");
  s.update("INSERT INTO learning_assessments(course_id,module_id,title,draft) SELECT s.course_id,m.id,'Synthetic assessment','{\"requiredForCompletion\":true}'::jsonb FROM learning_modules m JOIN learning_subjects s ON s.id=m.subject_id WHERE m.sort_order=1");
  s.update("INSERT INTO learning_assessment_versions(assessment_id,version_number,definition) SELECT id,1,draft FROM learning_assessments");
  s.update("UPDATE learning_assessments a SET status='PUBLISHED',current_version_id=v.id FROM learning_assessment_versions v WHERE v.assessment_id=a.id");
  s.update("INSERT INTO learning_assessment_attempts(id,assessment_id,version_id,student_id,attempt_number,status,answers,submission_key,earned_points,total_points,score_percent,passed,criteria,submitted_at) SELECT gen_random_uuid(),a.id,a.current_version_id,2,1,'SUBMITTED','{}',gen_random_uuid(),1,1,100,TRUE,'{}',CURRENT_TIMESTAMP FROM learning_assessments a");
  // Enough unrelated attempts to verify selectivity of the partial passed-attempt index.
  s.update("INSERT INTO learning_assessment_attempts(id,assessment_id,version_id,student_id,attempt_number,status) SELECT gen_random_uuid(),a.id,a.current_version_id,u.id,1,'IN_PROGRESS' FROM learning_assessments a CROSS JOIN student_users u WHERE u.id<>2");
  for(var jdbc:List.of(o,c,s))FixtureStatistics.analyze(jdbc);
 }
 private Map<String,Object> measureLookup(Callable<?> operation)throws Exception{for(int i=0;i<3;i++)operation.call();long[] samples=new long[10];for(int i=0;i<samples.length;i++){long start=System.nanoTime();operation.call();samples[i]=System.nanoTime()-start;}Arrays.sort(samples);return Map.of("rounds",10,"medianMs",samples[5]/1e6,"p95Ms",samples[9]/1e6);}
 private Map<String,Object> measure(Callable<Map<String,Object>> operation,Counted counted)throws Exception{
  for(int i=0;i<3;i++)operation.call();long[] nanos=new long[10];counted.calls.set(0);counted.rows.set(0);
  for(int i=0;i<nanos.length;i++){long started=System.nanoTime();operation.call();nanos[i]=System.nanoTime()-started;}
  Arrays.sort(nanos);return Map.of("rounds",10,"medianMs",nanos[5]/1e6,"p95Ms",nanos[9]/1e6,"queriesPerCall",counted.calls.get()/10,"rowsPerCall",counted.rows.get()/10);
 }
 private double plan(JdbcTemplate jdbc,String sql,String name,Object... parameters)throws Exception{
  String json=jdbc.queryForObject("EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON) "+sql,String.class,parameters);Files.writeString(output.resolve(name+".json"),json);return JSON.readTree(json).get(0).path("Execution Time").asDouble();
 }

 private void runHttp(List<Store> stores,Map<String,Object> evidence)throws Exception{
  var arguments=new ArrayList<String>(List.of("--server.port=0","--management.server.port=0","--spring.profiles.active=test","--spring.jpa.hibernate.ddl-auto=validate","--app.mail.enabled=false","--operations.catalog-jobs-enabled=false","--future.poll-enabled=false","--passport.delivery-enabled=false","--spring.task.scheduling.enabled=false"));
  for(var s:stores){String prefix=s.trail().equals("oportunidades")?"spring.datasource":"trampolim.datasource."+s.trail();arguments.add("--"+prefix+".url="+s.url()+"?currentSchema="+s.schema());arguments.add("--"+prefix+".username="+s.user());arguments.add("--"+prefix+".password="+s.password());arguments.add("--"+prefix+".hikari.maximum-pool-size=6");arguments.add("--"+prefix+".hikari.connection-timeout=5000");}
  try(var context=new SpringApplicationBuilder(BackendApplication.class).run(arguments.toArray(String[]::new))){
   String base="http://127.0.0.1:"+context.getEnvironment().getRequiredProperty("local.server.port");
   var users=context.getBean(com.trampolim.api.user.AppUserRepository.class);
   evidence.put("authenticationLookupBefore",measureLookup(()->users.findByEmail("candidate@performance.test").orElseThrow()));
   evidence.put("authenticationLookupAfter",measureLookup(()->users.findSessionByEmail("candidate@performance.test").orElseThrow()));
   evidence.put("avoidedBinaryBytesPerAuthenticatedLookup",1310720);
   var tokens=context.getBean(TrailTokenService.class);
   String candidate=tokens.issue(TrailTokenService.Trail.OPORTUNIDADES,"candidate@performance.test",1,0),company=tokens.issue(TrailTokenService.Trail.EMPRESAS,"company1@performance.test",1,0),student=tokens.issue(TrailTokenService.Trail.CAPACITA,"student2@performance.test",2,0);
   List<String> credentials=List.of(candidate,company,student,student);
   List<String> routes=List.of("/api/opportunities/job/regional?limit=20&municipalityCode=2914604&includeRemote=false&area=admin&modality=Remoto&q=SQL","/api/company/listings?limit=20&page=0","/api/capacita/learning/performance","/api/capacita/learning/courses?limit=10&offset=0&q=Fixture");
   for(int i=0;i<4;i++)assertEquals(200,get(base+routes.get(i),credentials.get(i)).statusCode());
   var first=JSON.readTree(get(base+routes.get(1),company).body());var second=JSON.readTree(get(base+routes.get(1).replace("page=0","page=1"),company).body());assertEquals(20,first.size());assertEquals(20,second.size());var firstIds=new HashSet<Long>();first.forEach(row->firstIds.add(row.path("id").asLong()));second.forEach(row->assertFalse(firstIds.contains(row.path("id").asLong())));
   var companyJdbc=stores.get(1).jdbc();for(var row:first)assertEquals(1L,companyJdbc.queryForObject("SELECT company_id FROM company_listings WHERE id=?",Long.class,row.path("id").asLong()));
   assertEquals(400,get(base+"/api/company/listings?limit=201",company).statusCode());assertTrue(Set.of(401,403).contains(get(base+routes.get(1),candidate).statusCode()));
   // Real repository query must filter before slicing, including unapproved and expired records.
   var publicPage=JSON.readTree(get(base+"/api/opportunities/job?limit=20&page=0",candidate).body());assertEquals(20,publicPage.size());for(var item:publicPage){long id=item.path("id").asLong();assertEquals(1,stores.get(0).jdbc().queryForObject("SELECT count(*) FROM catalog_items WHERE id=? AND active=TRUE AND moderation_status='APPROVED' AND expires_at>=CURRENT_DATE",Integer.class,id));}
   var pools=List.of(context.getBean("dataSource",HikariDataSource.class),context.getBean("empresasDataSource",HikariDataSource.class),context.getBean("capacitaDataSource",HikariDataSource.class));
   var geo=context.getBean(com.trampolim.api.geography.TerritorialCatalog.class);
   var combined=com.trampolim.api.geography.RegionalQuery.filter(geo,new com.trampolim.api.geography.RegionalQuery.Filters("","2914604",false,"249","","admin","Remoto",Long.MAX_VALUE,20),"l");var params=new ArrayList<Object>(combined.args());params.add(21);
   evidence.put("complexFilterPlanMs",plan(context.getBean("jdbcTemplate",JdbcTemplate.class),"SELECT l.id,l.title,l.description,l.provider FROM catalog_items l WHERE l.kind='job' AND l.active=TRUE AND l.moderation_status='APPROVED' AND (l.expires_at IS NULL OR l.expires_at>=(CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date)"+combined.where()+" ORDER BY l.id DESC LIMIT ?","complex-filters-after",params.toArray()));
   evidence.put("complexFilterRead",load(base,List.of(routes.get(0).replace("q=SQL","q=249")),List.of(candidate),4,48,Duration.ZERO,pools));
   evidence.put("steadyLoad",load(base,routes,credentials,8,320,Duration.ZERO,pools));
   evidence.put("burstLoad",load(base,routes,credentials,32,320,Duration.ZERO,pools));
   int soakSeconds=Integer.getInteger("audit.performance.soak-seconds",20);if(soakSeconds<1||soakSeconds>3600)throw new IllegalArgumentException("Soak duration must be between 1 and 3600 seconds");
   evidence.put("shortSoak",load(base,routes,credentials,12,0,Duration.ofSeconds(soakSeconds),pools));
   // Cancel a real SQL statement and demonstrate that the pooled connection remains reusable.
   var timed=new JdbcTemplate(pools.get(2));timed.setQueryTimeout(1);assertThrows(org.springframework.dao.QueryTimeoutException.class,()->timed.execute("SELECT pg_sleep(3)"));assertEquals(1,timed.queryForObject("SELECT 1",Integer.class));
   // Exhaust only Capacita's pool: fail boundedly while Empresas stays available, then recover.
   var held=new ArrayList<Connection>();try{for(int i=0;i<6;i++)held.add(pools.get(2).getConnection());long started=System.nanoTime();assertEquals(503,get(base+routes.get(2),student).statusCode());evidence.put("poolExhaustionResponseMs",(System.nanoTime()-started)/1e6);assertEquals(200,get(base+"/api/company/dashboard",company).statusCode());}finally{for(var connection:held)connection.close();}
   assertEquals(200,get(base+routes.get(2),student).statusCode());evidence.put("poolExhaustionIsolatedAndRecovered",true);evidence.put("queryCancellationRecovered",true);
  }
 }
 private HttpResponse<String> get(String uri,String token)throws Exception{return http.send(HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());}
 private Map<String,Object> load(String base,List<String> routes,List<String> tokens,int concurrency,int count,Duration duration,List<HikariDataSource> pools)throws Exception{
  var latencies=new ConcurrentLinkedQueue<Double>();var statuses=new ConcurrentHashMap<Integer,AtomicInteger>();var serial=new AtomicInteger();var failure=new AtomicInteger();var maxActive=new AtomicInteger();var maxWaiting=new AtomicInteger();var maxHeap=new AtomicLong();var running=new AtomicBoolean(true);
  long started=System.nanoTime(),deadline=started+duration.toNanos(),cpuBefore=cpu(),gcBefore=gc();
  try(var sampler=Executors.newSingleThreadScheduledExecutor();var workers=Executors.newFixedThreadPool(concurrency)){
   sampler.scheduleAtFixedRate(()->{if(!running.get())return;for(var pool:pools){var bean=pool.getHikariPoolMXBean();if(bean!=null){maxActive.accumulateAndGet(bean.getActiveConnections(),Math::max);maxWaiting.accumulateAndGet(bean.getThreadsAwaitingConnection(),Math::max);}}maxHeap.accumulateAndGet(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(),Math::max);},0,20,TimeUnit.MILLISECONDS);
   var futures=new ArrayList<Future<?>>();for(int w=0;w<concurrency;w++)futures.add(workers.submit(()->{while(true){int n=serial.getAndIncrement();if(duration.isZero()?n>=count:System.nanoTime()>=deadline)break;long begin=System.nanoTime();try{int index=n%routes.size();var response=get(base+routes.get(index),tokens.get(index));statuses.computeIfAbsent(response.statusCode(),ignored->new AtomicInteger()).incrementAndGet();if(response.statusCode()!=200)failure.incrementAndGet();else{var body=JSON.readTree(response.body());if(index==0&&body.path("items").size()>20||index==1&&(!body.isArray()||body.size()>20)||index==3&&(!body.isArray()||body.size()>10))failure.incrementAndGet();}}catch(Exception error){failure.incrementAndGet();}latencies.add((System.nanoTime()-begin)/1e6);}}));
   for(var future:futures)future.get(Math.max(120,duration.toSeconds()+60),TimeUnit.SECONDS);running.set(false);sampler.shutdownNow();
  }
  double elapsed=(System.nanoTime()-started)/1e9;var sorted=latencies.stream().mapToDouble(Double::doubleValue).sorted().toArray();var result=new LinkedHashMap<String,Object>();result.put("clients",concurrency);result.put("requests",sorted.length);result.put("elapsedSeconds",elapsed);result.put("requestsPerSecond",sorted.length/elapsed);result.put("p50Ms",percentile(sorted,.5));result.put("p95Ms",percentile(sorted,.95));result.put("p99Ms",percentile(sorted,.99));result.put("maxMs",sorted[sorted.length-1]);result.put("errors",failure.get());var codes=new TreeMap<String,Integer>();statuses.forEach((code,number)->codes.put(code.toString(),number.get()));result.put("statusCodes",codes);result.put("maxActiveConnectionsPerPool",maxActive.get());result.put("maxWaitingForConnectionPerPool",maxWaiting.get());result.put("maxSampledJvmHeapBytes",maxHeap.get());result.put("jvmCpuSeconds",(cpu()-cpuBefore)/1e9);result.put("gcCollections",gc()-gcBefore);
  Files.writeString(output.resolve("last-load-phase.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));assertEquals(0,failure.get(),result.toString());return result;
 }
 private static double percentile(double[] values,double fraction){return values[Math.min(values.length-1,(int)Math.ceil(values.length*fraction)-1)];}
 private static long cpu(){return ((com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();}
 private static long gc(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
 /** Counts successful JDBC executions and consumed rows, with the same Hikari pool for both versions. */
 private static final class Counted extends AbstractDataSource {
  final AtomicLong calls=new AtomicLong(),rows=new AtomicLong();private final DataSource delegate;
  Counted(DataSource delegate){this.delegate=delegate;}
  @Override public Connection getConnection()throws SQLException{return wrap(delegate.getConnection(),Connection.class);}
  @Override public Connection getConnection(String user,String password)throws SQLException{return wrap(delegate.getConnection(user,password),Connection.class);}
  @SuppressWarnings("unchecked") private <T>T wrap(T target,Class<T> contract){return (T)java.lang.reflect.Proxy.newProxyInstance(contract.getClassLoader(),new Class<?>[]{contract},(proxy,method,args)->{Object value;try{value=method.invoke(target,args);}catch(InvocationTargetException error){throw error.getCause();}if(target instanceof Statement&&method.getName().startsWith("execute"))calls.incrementAndGet();if(target instanceof ResultSet&&method.getName().equals("next")&&Boolean.TRUE.equals(value))rows.incrementAndGet();if(value instanceof PreparedStatement statement)return wrap(statement,PreparedStatement.class);if(value instanceof Statement statement)return wrap(statement,Statement.class);if(value instanceof ResultSet result)return wrap(result,ResultSet.class);return value;});}
 }
}
