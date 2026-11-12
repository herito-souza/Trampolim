package com.trampolim.api.planning;

import java.util.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.radar.*;
import com.trampolim.api.security.BoundedRateLimiter;
import static com.trampolim.api.planning.EmployabilityPlanModel.*;
import static com.trampolim.api.planning.EmployabilityPlanEngine.*;

@Service
public class EmployabilityPlanService {
 private final JdbcTemplate jdbc;private final SkillCatalog users;private final RadarService radar;private final RadarBridge json;private final EmployabilityPlanEngine engine;private final TransactionTemplate tx,readTx;
 private final BoundedRateLimiter limits=new BoundedRateLimiter();
 public EmployabilityPlanService(@Qualifier("jdbcTemplate")JdbcTemplate jdbc,SkillCatalog users,RadarService radar,RadarBridge json,EmployabilityPlanEngine engine,@Qualifier("oportunidadesTransactionManager")PlatformTransactionManager manager){this.jdbc=jdbc;this.users=users;this.radar=radar;this.json=json;this.engine=engine;tx=new TransactionTemplate(manager);readTx=new TransactionTemplate(manager);readTx.setReadOnly(true);readTx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);}
 private void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private void conflict(){throw new ResponseStatusException(HttpStatus.CONFLICT,"O plano ou seus dados mudaram. Atualize os dados antes de confirmar a operação.");}
 private void version(Long value){if(value==null||value<0)bad("Informe expectedRevision não negativa.");}
 private Objectives validate(Objectives input){
  if(input==null||input.skills()==null||input.jobs()==null||input.skills().size()>10||input.jobs().size()>3)bad("Informe o objetivo, até dez competências e até três vagas de referência.");
  String occupation=RadarModel.text(input.occupation(),180),area=RadarModel.text(input.area(),120),city=RadarModel.text(input.city(),180),modality=RadarModel.text(input.modality(),40);
  if(!modality.isEmpty()&&!Set.of("Presencial","Híbrido","Remoto","Online").contains(modality))bad("Modalidade inválida.");
  if(occupation.isEmpty()&&area.isEmpty()&&input.skills().isEmpty()&&input.jobs().isEmpty())bad("Informe uma ocupação, área, competência ou vaga de referência.");
  var codes=new HashSet<String>();for(var skill:input.skills())if(skill==null||skill.skillCode()==null||!codes.add(skill.skillCode())||skill.level()<1||skill.level()>4)bad("Competências sem duplicação, com nível de 1 a 4.");users.validateCodes(codes);
  var keys=new HashSet<String>();for(var job:input.jobs())if(job==null||!Set.of("catalog","company").contains(Objects.toString(job.source(),""))||job.id()<1||!keys.add(job.source()+":"+job.id()))bad("Vagas de referência inválidas ou duplicadas.");
  return new Objectives(occupation,area,city,modality,input.skills().stream().sorted(Comparator.comparing(SkillGoal::skillCode)).map(s->new SkillGoal(s.skillCode(),s.level(),Boolean.TRUE.equals(s.requireEvidence()))).toList(),input.jobs().stream().sorted(Comparator.comparing(JobRef::source).thenComparingLong(JobRef::id)).toList());
 }
 private void lockUser(long uid){if(jdbc.queryForList("SELECT id FROM app_users WHERE id=? AND email_verified=TRUE AND account_type='PERSON' FOR UPDATE",uid).isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Use uma conta pessoal verificada de Oportunidades.");}
 private Map<String,Object> stored(long uid,boolean lock){return jdbc.queryForList("SELECT * FROM individual_employability_plans WHERE user_id=?"+(lock?" FOR UPDATE":""),uid).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Crie e revise seus objetivos antes de gerar o plano."));}
 private Objectives goals(Map<String,Object> row){return json.convert(json.read(row.get("objectives")),Objectives.class);}
 private String inputs(long uid,Objectives goals){
  var values=new ArrayList<Object>();values.add(goals);values.add(YearMonth.now().toString());
  values.add(jdbc.queryForMap("SELECT COALESCE(diagnostic_data,'') AS diagnostic FROM app_users WHERE id=?",uid));
  values.add(jdbc.queryForList("SELECT skill_code,level,updated_at FROM candidate_skill_declarations WHERE user_id=? ORDER BY skill_code",uid));
  values.add(jdbc.queryForList("SELECT receipt_id,skill_code,level,awarded_at FROM candidate_skill_evidence WHERE user_id=? ORDER BY receipt_id",uid));
  values.add(jdbc.queryForList("SELECT city,modality,area,experience_confirmed FROM candidate_radar_preferences WHERE user_id=?",uid));
  values.add(jdbc.queryForList("SELECT start_date,end_date,is_current FROM experiences WHERE user_id=? ORDER BY id LIMIT 201",uid));
  values.add(jdbc.queryForList("SELECT level,in_progress FROM educations WHERE user_id=? ORDER BY id LIMIT 201",uid));
  values.add(jdbc.queryForMap("SELECT revision FROM radar_scoring_policy WHERE id=1"));return PlanHashes.hash(values);
 }
 private Map<String,Object> diagnostic(long uid){
  String data=jdbc.queryForObject("SELECT diagnostic_data FROM app_users WHERE id=?",String.class,uid);
  var result=new LinkedHashMap<String,Object>();result.put("available",false);result.put("notice","Diagnóstico orienta interesses, não comprova competência nem determina a ocupação.");
  if(data!=null)try{var node=json.read(data);if(node.path("answers").isArray()&&node.path("answers").size()==10&&node.path("scores").isObject()){result.put("available",true);result.put("trackId",node.path("trackId").asText("").substring(0,Math.min(64,node.path("trackId").asText("").length())));result.put("trackName",node.path("trackName").asText("").substring(0,Math.min(180,node.path("trackName").asText("").length())));}}catch(RuntimeException invalid){result.put("status","NEEDS_REVIEW");}
  return result;
 }
 public Map<String,Object> get(String email){long uid=users.user(email);return view(uid,email);}
 private Map<String,Object> view(long uid,String email){return readTx.execute(status->viewSnapshot(uid,email));}
 private Map<String,Object> viewSnapshot(long uid,String email){
  var rows=jdbc.queryForList("SELECT * FROM individual_employability_plans WHERE user_id=?",uid);
  if(rows.isEmpty())return Map.of("exists",false,"revision",0,"diagnostic",diagnostic(uid),"suggestedObjectives",radar.preferencesForUser(email));
  var row=rows.getFirst();var result=new LinkedHashMap<String,Object>();var objectives=goals(row);result.put("exists",true);result.put("revision",row.get("revision"));result.put("generation",row.get("generation"));result.put("objectives",objectives);result.put("generatedAt",row.get("generated_at"));
  boolean stale=row.get("inputs_hash")==null||!inputs(uid,objectives).equals(row.get("inputs_hash"));result.put("localInputsChanged",stale);result.put("sourceFreshness","SNAPSHOT_NOT_LIVE");result.put("snapshot",row.get("snapshot")==null?null:json.convert(json.read(row.get("snapshot")),Map.class));
  var progress=jdbc.queryForList("SELECT step_key,state,criterion_hash,updated_at FROM individual_employability_steps WHERE user_id=? AND active=TRUE ORDER BY step_key",uid);result.put("progress",progress);result.put("declaredDone",progress.stream().filter(s->"DECLARED_DONE".equals(s.get("state"))).count());result.put("activeSteps",progress.size());return result;
 }
 public Map<String,Object> update(String email,Update input){long uid=users.user(email);if(input==null)bad("Informe seus objetivos e versão.");version(input.expectedRevision());var objectives=validate(input.objectives());
  tx.executeWithoutResult(status->{lockUser(uid);var existing=jdbc.queryForList("SELECT revision FROM individual_employability_plans WHERE user_id=? FOR UPDATE",uid);long current=existing.isEmpty()?0:((Number)existing.getFirst().get("revision")).longValue();if(current!=input.expectedRevision())conflict();
   jdbc.update("INSERT INTO individual_employability_plans(user_id,objectives) VALUES (?,?::jsonb) ON CONFLICT(user_id) DO UPDATE SET objectives=EXCLUDED.objectives,revision=individual_employability_plans.revision+1,snapshot=NULL,inputs_hash=NULL,generated_at=NULL,updated_at=CURRENT_TIMESTAMP",uid,json.encode(objectives));jdbc.update("UPDATE individual_employability_steps SET active=FALSE WHERE user_id=?",uid);users.audit(uid,"EMPLOYABILITY_OBJECTIVES_UPDATED");});return view(uid,email);
 }
 private Map<String,Object> prior(long uid,Refresh input){return jdbc.queryForList("SELECT generation,produced_revision,request_hash FROM individual_employability_runs WHERE user_id=? AND request_key=?",uid,input.requestKey()).stream().findFirst().orElse(null);}
 public Map<String,Object> refresh(String email,Refresh input){
  long uid=users.user(email);if(input==null||input.requestKey()==null)bad("Informe versão e requestKey.");version(input.expectedRevision());String requestHash=RadarBridge.hash("REFRESH:"+input.expectedRevision());
  var previous=prior(uid,input);if(previous!=null){if(!requestHash.equals(previous.get("request_hash")))conflict();return result(uid,email,previous,true);}
  var initial=stored(uid,false);if(((Number)initial.get("revision")).longValue()!=input.expectedRevision())conflict();
  if(!limits.allow(Long.toString(uid),10,Duration.ofHours(1)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde antes de recalcular o plano.");
  var objectives=goals(initial);String fingerprint=inputs(uid,objectives);var analyses=new ArrayList<Map<String,Object>>();var sources=new ArrayList<Map<String,Object>>();var jobs=new ArrayList<>(objectives.jobs());
  if(jobs.isEmpty()){
   String q=objectives.occupation().isBlank()?objectives.area():objectives.occupation();
   if(!q.isBlank())for(String source:List.of("catalog","company"))try{var found=radar.jobs(email,source,Long.MAX_VALUE,3,q.substring(0,Math.min(120,q.length())));for(var item:rows(found.get("items")))if(item.get("id") instanceof Number id&&id.longValue()>0&&jobs.size()<3)jobs.add(new JobRef(source,id.longValue()));sources.add(Map.of("source",source,"status","AVAILABLE"));}catch(ResponseStatusException error){sources.add(Map.of("source",source,"status","UNAVAILABLE"));}
  }
  for(var job:jobs)try{analyses.add(radar.match(email,job.source(),job.id()));sources.add(Map.of("source",job.source()+":"+job.id(),"status","AVAILABLE"));}catch(ResponseStatusException error){sources.add(Map.of("source",job.source()+":"+job.id(),"status",error.getStatusCode().value()==404?"NOT_AVAILABLE":"UNAVAILABLE"));}
  if(!objectives.skills().isEmpty())analyses.add(radar.analyzeObjectives(email,objectives.skills().stream().map(s->new RadarModel.Need(s.skillCode(),s.level(),true,Boolean.TRUE.equals(s.requireEvidence()),false)).toList()));
  var snapshot=engine.build(objectives,diagnostic(uid),analyses,sources);
  if(!fingerprint.equals(inputs(uid,objectives)))conflict();
  var committed=tx.execute(status->{lockUser(uid);var current=stored(uid,true);var replay=prior(uid,input);if(replay!=null){if(!requestHash.equals(replay.get("request_hash")))conflict();return replay;}
   if(((Number)current.get("revision")).longValue()!=input.expectedRevision()||!fingerprint.equals(inputs(uid,objectives)))conflict();
   if(jdbc.queryForObject("SELECT count(*) FROM radar_audit WHERE actor_id=? AND event='EMPLOYABILITY_PLAN_GENERATED' AND created_at>CURRENT_TIMESTAMP-INTERVAL '1 hour'",Integer.class,uid)>=5)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite de cinco gerações por hora. Seu plano salvo continua disponível.");
   long revision=input.expectedRevision()+1,generation=((Number)current.get("generation")).longValue()+1;
   jdbc.update("UPDATE individual_employability_plans SET revision=?,generation=?,snapshot=?::jsonb,inputs_hash=?,generated_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE user_id=?",revision,generation,json.encode(snapshot),fingerprint,uid);
   jdbc.update("UPDATE individual_employability_steps SET active=FALSE WHERE user_id=?",uid);
   for(var step:rows(snapshot.get("steps")))jdbc.update("INSERT INTO individual_employability_steps(user_id,step_key,criterion_hash) VALUES (?,?,?) ON CONFLICT(user_id,step_key) DO UPDATE SET criterion_hash=EXCLUDED.criterion_hash,state=CASE WHEN individual_employability_steps.criterion_hash=EXCLUDED.criterion_hash THEN individual_employability_steps.state ELSE 'NOT_STARTED' END,active=TRUE,updated_at=CURRENT_TIMESTAMP",uid,step.get("key"),step.get("criterionHash"));
   jdbc.update("INSERT INTO individual_employability_runs(user_id,request_key,request_hash,generation,produced_revision,snapshot) VALUES (?,?,?,?,?,?::jsonb)",uid,input.requestKey(),requestHash,generation,revision,json.encode(snapshot));
   jdbc.update("DELETE FROM individual_employability_runs WHERE user_id=? AND generation IN (SELECT generation FROM individual_employability_runs WHERE user_id=? ORDER BY generation DESC OFFSET 20)",uid,uid);
   jdbc.update("DELETE FROM individual_employability_steps WHERE user_id=? AND active=FALSE AND step_key IN (SELECT step_key FROM individual_employability_steps WHERE user_id=? AND active=FALSE ORDER BY updated_at DESC,step_key OFFSET 200)",uid,uid);users.audit(uid,"EMPLOYABILITY_PLAN_GENERATED");return Map.<String,Object>of("generation",generation,"produced_revision",revision);});
  return result(uid,email,committed,committed.containsKey("request_hash"));
 }
 private Map<String,Object> result(long uid,String email,Map<String,Object> run,boolean replay){return Map.of("replayed",replay,"generatedRevision",run.get("produced_revision"),"generation",run.get("generation"),"plan",view(uid,email));}
 public Map<String,Object> progress(String email,String key,Progress input){long uid=users.user(email);if(input==null||key==null||!key.matches("[a-f0-9]{64}")||!Set.of("NOT_STARTED","IN_PROGRESS","DECLARED_DONE","DISMISSED").contains(Objects.toString(input.state(),"")))bad("Etapa ou progresso declarado inválido.");version(input.expectedRevision());
  tx.executeWithoutResult(status->{lockUser(uid);var plan=stored(uid,true);if(((Number)plan.get("revision")).longValue()!=input.expectedRevision()||!Objects.equals(plan.get("inputs_hash"),inputs(uid,goals(plan))))conflict();if(jdbc.update("UPDATE individual_employability_steps SET state=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=? AND step_key=? AND active=TRUE",input.state(),uid,key)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Etapa não encontrada no plano atual.");jdbc.update("UPDATE individual_employability_plans SET revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE user_id=?",uid);users.audit(uid,"EMPLOYABILITY_PROGRESS_DECLARED");});return view(uid,email);
 }
 public Map<String,Object> history(String email,long before,int limit){long uid=users.user(email);if(before<1||limit<1||limit>20)bad("Consulta inválida.");var items=jdbc.queryForList("SELECT generation,produced_revision,created_at,snapshot FROM individual_employability_runs WHERE user_id=? AND generation<? ORDER BY generation DESC LIMIT ?",uid,before,limit+1);boolean more=items.size()>limit;items=items.stream().limit(limit).toList();for(var item:items)item.put("snapshot",json.convert(json.read(item.get("snapshot")),Map.class));var result=new LinkedHashMap<String,Object>();result.put("items",items);result.put("nextCursor",more?items.getLast().get("generation"):null);return result;}
 public void delete(String email,long expected){long uid=users.user(email);if(expected<0)bad("Versão inválida.");tx.executeWithoutResult(status->{lockUser(uid);var plan=stored(uid,true);if(((Number)plan.get("revision")).longValue()!=expected)conflict();jdbc.update("DELETE FROM individual_employability_plans WHERE user_id=?",uid);users.audit(uid,"EMPLOYABILITY_PLAN_DELETED");});}
}
