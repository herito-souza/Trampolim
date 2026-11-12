package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class LearningProgressSummaryTest {
 @Test void requiredModuleAndCourseAssessmentsAreNotInterchangeable(){
 var courses=new ArrayList<Map<String,Object>>(List.of(new HashMap<>(Map.of("id",1L)),new HashMap<>(Map.of("id",2L)),new HashMap<>(Map.of("id",3L))));
 var modules=List.<Map<String,Object>>of(Map.of("course_id",1L,"module_id",10L,"lessons_total",2L,"lessons_completed",2L),Map.of("course_id",1L,"module_id",11L,"lessons_total",3L,"lessons_completed",3L),Map.of("course_id",2L,"module_id",20L,"lessons_total",1L,"lessons_completed",1L));
 var courseTest=new HashMap<String,Object>(Map.of("course_id",2L,"required_total",1L,"passed_total",0L));courseTest.put("module_id",null);
 var tests=List.<Map<String,Object>>of(Map.of("course_id",1L,"module_id",10L,"required_total",2L,"passed_total",1L),courseTest);
 var totals=LearningProgressSummary.apply(courses,modules,tests);
 assertEquals(0,totals.courses());assertEquals(6,totals.lessons());assertEquals(2,totals.modules());assertEquals(1L,courses.get(0).get("modulesCompleted"));assertEquals(1L,courses.get(1).get("modulesCompleted"));assertEquals(false,courses.get(2).get("completed"));assertEquals(0L,courses.get(2).get("lessonsTotal"));
 }
}
