package com.trampolim.api.planning;

import java.util.*;
import org.springframework.stereotype.Component;
import com.trampolim.api.radar.RadarBridge;
import static com.trampolim.api.planning.EmployabilityPlanModel.*;

/** Rule set 1: professional inputs only. No text profiling, generative AI or hiring decision. */
@Component
public class EmployabilityPlanEngine {
 public Map<String,Object> build(Objectives goals,Map<String,Object> diagnostic,List<Map<String,Object>> analyses,List<Map<String,Object>> sources){
  var steps=new LinkedHashMap<String,Map<String,Object>>();
  add(steps,"REVIEW_OBJECTIVES","objectives","Revisar meu objetivo profissional",Map.of("rule","USER_REVIEW","occupation",goals.occupation(),"area",goals.area(),"city",goals.city(),"modality",goals.modality()),Map.of());
  if(!goals.area().isBlank()||!goals.city().isBlank()||!goals.modality().isBlank())add(steps,"REVIEW_PREFERENCES","radar-preferences","Revisar minhas preferências no Radar",Map.of("rule","VOLUNTARY_PREFERENCE_REVIEW","area",goals.area(),"city",goals.city(),"modality",goals.modality()),Map.of("endpoint","/api/radar/preferences","notice","Objetivos do plano não alteram silenciosamente suas preferências do Radar."));
  if(!Boolean.TRUE.equals(diagnostic.get("available")))add(steps,"DIAGNOSTIC","diagnostic","Realizar ou revisar o diagnóstico vocacional",Map.of("rule","DIAGNOSTIC_NOT_SAVED","notice","Interesses não comprovam proficiência."),Map.of());
  var gaps=new TreeMap<String,Map<String,Object>>();var training=new TreeMap<String,Map<String,Object>>();
  boolean partial=sources.stream().anyMatch(r->!"AVAILABLE".equals(r.get("status")));
  for(var result:analyses){
   var analysis=map(result.get("analysis"));boolean evidenceUnavailable="UNAVAILABLE".equals(result.get("evidenceStatus"));partial|=evidenceUnavailable;
   for(var row:rows(analysis.get("skills"))){
    if("MET".equals(row.get("status")))continue;
    String code=Objects.toString(row.get("skillCode"),"");if(!code.matches("[a-z][a-z0-9-]{1,63}"))continue;
    var prior=gaps.computeIfAbsent(code,key->new LinkedHashMap<>(Map.of("skillCode",code,"requiredLevel",0,"requiresEvidence",false,"mandatory",false,"evidenceUnavailable",false)));
    prior.put("requiredLevel",Math.max(number(prior.get("requiredLevel")),number(row.get("requiredLevel"))));
    for(String flag:List.of("requiresEvidence","mandatory"))prior.put(flag,Boolean.TRUE.equals(prior.get(flag))||Boolean.TRUE.equals(row.get(flag)));
    prior.put("evidenceUnavailable",Boolean.TRUE.equals(prior.get("evidenceUnavailable"))||evidenceUnavailable&&Boolean.TRUE.equals(row.get("requiresEvidence")));
   }
   for(var factor:rows(analysis.get("factors")))if(Set.of("EXPERIENCE","EDUCATION").contains(factor.get("key"))&&!Set.of("MET","NOT_APPLICABLE").contains(factor.get("status"))){
    String kind=Objects.toString(factor.get("key"));add(steps,"PROFILE_REVIEW",kind,"Revisar informações de "+(kind.equals("EXPERIENCE")?"experiência":"formação"),Map.of("rule","RADAR_PROFESSIONAL_FACTOR","factor",kind),Map.of("explanation",Objects.toString(factor.get("explanation"),"")));
   }
   var offer=map(result.get("training"));if("UNAVAILABLE".equals(offer.get("status")))partial=true;
   for(var row:rows(offer.get("items")).stream().limit(40).toList()){
    long course=identifier(row.get("course_id")),module=identifier(row.get("module_id"));String code=Objects.toString(row.get("skill_code"),"");int level=number(row.get("level"));
    if(course<1||module<1||code.isBlank()||level<1||level>4||Objects.toString(row.get("course_title"),"").isBlank())continue;
    String key=course+":"+module+":"+code;var existing=training.get(key);if(existing==null||number(existing.get("level"))<level)training.put(key,row);
   }
  }
  Set<String> personalGoals=new HashSet<>();goals.skills().forEach(goal->personalGoals.add(goal.skillCode()));
  var selected=gaps.values().stream().sorted(Comparator.<Map<String,Object>>comparingInt(r->personalGoals.contains(r.get("skillCode"))?0:1).thenComparingInt(r->Boolean.TRUE.equals(r.get("mandatory"))?0:1).thenComparing(r->r.get("skillCode").toString())).limit(40).toList();
  Set<String> selectedCodes=new HashSet<>();
  for(var gap:selected){String code=gap.get("skillCode").toString();boolean pending=Boolean.TRUE.equals(gap.get("evidenceUnavailable"));if(!pending)selectedCodes.add(code);add(steps,pending?"EVIDENCE_REVIEW":"SKILL_DEVELOPMENT",code,pending?"Revalidar comprovação de "+code:"Desenvolver "+code,Map.copyOf(gap),Map.of("notice","Concluir esta etapa por declaração não comprova a competência. A evidência é validada pelo Radar."));}
  var groups=new TreeMap<String,List<Map<String,Object>>>();for(var offer:training.values())if(selectedCodes.contains(offer.get("skill_code")))groups.computeIfAbsent(offer.get("course_id")+":"+offer.get("module_id"),key->new ArrayList<>()).add(offer);
  int included=0;for(var group:groups.values()){if(included++>=20)break;var first=group.getFirst();var coverage=group.stream().map(row->Map.of("skillCode",row.get("skill_code"),"offeredLevel",row.get("level"),"requiredLevel",gaps.get(row.get("skill_code")).get("requiredLevel"),"coverage",number(row.get("level"))>=number(gaps.get(row.get("skill_code")).get("requiredLevel"))?"TARGET_LEVEL":"PARTIAL")).toList();
   add(steps,"TRAINING",first.get("course_id")+":"+first.get("module_id"),"Consultar curso: "+first.get("course_title"),Map.of("rule","PUBLISHED_TRAINING_FOR_GAPS","courseId",first.get("course_id"),"moduleId",first.get("module_id"),"coverage",coverage),Map.of("courseTitle",first.get("course_title"),"moduleTitle",Objects.toString(first.get("module_title"),""),"notice","Disponibilidade observada na geração. Acesse o Capacita com sua conta independente; participação não comprova aprovação."));
  }
  if(analyses.stream().noneMatch(a->a.containsKey("job")))add(steps,"SEARCH_OPPORTUNITIES","search","Pesquisar e escolher oportunidades compatíveis",Map.of("rule","NO_AVAILABLE_JOB_ANALYSIS"),Map.of("notice","A ausência de resultados não significa ausência de vagas no mercado."));
  var result=new LinkedHashMap<String,Object>();result.put("ruleVersion",1);result.put("objectives",goals);result.put("diagnostic",diagnostic);result.put("analyses",analyses);result.put("sources",sources);result.put("steps",List.copyOf(steps.values()));result.put("partial",partial);result.put("truncated",gaps.size()>40||groups.size()>20);result.put("notice","Plano orientativo, revisável e sem garantia de contratação. Progresso declarado não é curso concluído, competência comprovada ou certificado. Diagnóstico registra interesses, não aptidão ou proficiência.");return result;
 }
 private void add(Map<String,Map<String,Object>> steps,String kind,String identity,String title,Map<String,Object> criteria,Map<String,Object> resource){
  String key=RadarBridge.hash(kind+":"+identity);var rule=new LinkedHashMap<String,Object>();rule.put("version",1);rule.putAll(criteria);
  steps.putIfAbsent(key,new LinkedHashMap<>(Map.of("key",key,"kind",kind,"title",title,"criteria",rule,"criterionHash",PlanHashes.hash(rule),"resource",resource)));
 }
 @SuppressWarnings("unchecked") static Map<String,Object> map(Object value){return value instanceof Map?(Map<String,Object>)value:Map.of();}
 @SuppressWarnings("unchecked") static List<Map<String,Object>> rows(Object value){return value instanceof List<?> list?list.stream().filter(Map.class::isInstance).map(v->(Map<String,Object>)v).toList():List.of();}
 static long identifier(Object value){if(!(value instanceof Number n))return 0;long id=n.longValue();return id>0&&n.doubleValue()==(double)id?id:0;}
 static int number(Object value){if(!(value instanceof Number n))return 0;int number=n.intValue();return n.doubleValue()==(double)number?number:0;}
}
