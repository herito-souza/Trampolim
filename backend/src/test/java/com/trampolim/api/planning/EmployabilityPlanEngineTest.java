package com.trampolim.api.planning;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.trampolim.api.planning.EmployabilityPlanEngine.*;
import static com.trampolim.api.planning.EmployabilityPlanModel.*;
class EmployabilityPlanEngineTest {
 private final EmployabilityPlanEngine engine=new EmployabilityPlanEngine();
 private Objectives goals(){return new Objectives("Objetivo de teste","","","",List.of(),List.of());}
 @Test void absentSourcesDoNotInventCoursesJobsOrSkills(){var snapshot=engine.build(goals(),Map.of("available",false),List.of(),List.of());assertTrue(rows(snapshot.get("steps")).stream().noneMatch(s->Set.of("TRAINING","SKILL_DEVELOPMENT").contains(s.get("kind"))));assertTrue(rows(snapshot.get("analyses")).isEmpty());}
 @Test void duplicatedOffersAndGapsBecomeOneStepWithExplicitPartialCoverage(){
  var gap=Map.<String,Object>of("skillCode","sql","requiredLevel",3,"status","MISSING","mandatory",true,"requiresEvidence",false);
  var offer=Map.<String,Object>of("course_id",3000000000L,"module_id",3000000001L,"course_title","Curso sintético","module_title","Módulo sintético","skill_code","sql","level",1);
  var analysis=Map.<String,Object>of("analysis",Map.of("skills",List.of(gap,gap)),"training",Map.of("status","AVAILABLE","items",List.of(offer,offer)));
  var steps=rows(engine.build(goals(),Map.of("available",true),List.of(analysis,analysis),List.of()).get("steps"));
  assertEquals(1,steps.stream().filter(s->s.get("kind").equals("SKILL_DEVELOPMENT")).count());assertEquals(1,steps.stream().filter(s->s.get("kind").equals("TRAINING")).count());
  var training=steps.stream().filter(s->s.get("kind").equals("TRAINING")).findFirst().orElseThrow();assertEquals("PARTIAL",rows(map(training.get("criteria")).get("coverage")).getFirst().get("coverage"));
 }
 @Test void unavailableEvidenceIsReviewNotAClaimOfMissingKnowledge(){var row=Map.<String,Object>of("skillCode","sql","requiredLevel",2,"status","MISSING","requiresEvidence",true);var result=engine.build(goals(),Map.of("available",true),List.of(Map.of("analysis",Map.of("skills",List.of(row)),"evidenceStatus","UNAVAILABLE")),List.of());assertEquals(true,result.get("partial"));assertTrue(rows(result.get("steps")).stream().anyMatch(s->s.get("kind").equals("EVIDENCE_REVIEW")));}
 @Test void hashesAreCanonicalAndDoNotDependOnMapInsertionOrder(){var a=new LinkedHashMap<String,Object>();a.put("a",1);a.put("b",List.of(Map.of("level",2,"skill","sql")));var b=new LinkedHashMap<String,Object>();b.put("b",List.of(Map.of("skill","sql","level",2)));b.put("a",1);assertEquals(PlanHashes.hash(a),PlanHashes.hash(b));}

 @Test void boundedPlanPrioritizesExplicitUserGoals(){var gaps=new ArrayList<Map<String,Object>>();for(int i=0;i<50;i++)gaps.add(Map.of("skillCode","skill-"+i,"requiredLevel",2,"status","MISSING","mandatory",true));gaps.add(Map.of("skillCode","zz-goal","requiredLevel",1,"status","MISSING"));var personal=new Objectives("Meta","","","",List.of(new SkillGoal("zz-goal",1,false)),List.of());var result=engine.build(personal,Map.of("available",true),List.of(Map.of("analysis",Map.of("skills",gaps))),List.of());assertEquals(true,result.get("truncated"));var steps=rows(result.get("steps"));assertEquals(40,steps.stream().filter(s->"SKILL_DEVELOPMENT".equals(s.get("kind"))).count());assertTrue(steps.stream().anyMatch(s->"zz-goal".equals(map(s.get("criteria")).get("skillCode"))));}
}
