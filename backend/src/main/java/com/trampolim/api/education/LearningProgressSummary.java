package com.trampolim.api.education;

import java.util.*;

/** Linear fold of bounded database aggregates. Does not cache private progress or change completion rules. */
final class LearningProgressSummary {
    record Totals(long courses,long lessons,long modules) {}
    private static final class CourseStats {
        long lessons,done,required,passed,modules,modulesDone;
        final Map<Long,Long> unpassedByModule=new HashMap<>();
    }
    static Totals apply(List<Map<String,Object>> courses,List<Map<String,Object>> modules,List<Map<String,Object>> assessments) {
        var grouped=new HashMap<Long,CourseStats>();
        for(var row:assessments){var stats=grouped.computeIfAbsent(number(row,"course_id"),ignored->new CourseStats());long required=number(row,"required_total"),passed=number(row,"passed_total");stats.required+=required;stats.passed+=passed;if(row.get("module_id")!=null)stats.unpassedByModule.merge(number(row,"module_id"),required-passed,Long::sum);}
        for(var row:modules){var stats=grouped.computeIfAbsent(number(row,"course_id"),ignored->new CourseStats());long total=number(row,"lessons_total"),done=number(row,"lessons_completed");stats.lessons+=total;stats.done+=done;stats.modules++;if(total>0&&done==total&&stats.unpassedByModule.getOrDefault(number(row,"module_id"),0L)==0)stats.modulesDone++;}
        long completed=0,lessonsDone=0,modulesDone=0;
        for(var course:courses){var stats=grouped.getOrDefault(number(course,"id"),new CourseStats());boolean complete=stats.lessons>0&&stats.done==stats.lessons&&stats.required==stats.passed;if(complete)completed++;lessonsDone+=stats.done;modulesDone+=stats.modulesDone;course.put("lessonsTotal",stats.lessons);course.put("lessonsCompleted",stats.done);course.put("modulesTotal",stats.modules);course.put("modulesCompleted",stats.modulesDone);course.put("requiredAssessments",stats.required);course.put("passedRequiredAssessments",stats.passed);course.put("progressPercent",stats.lessons==0?0:Math.round(100d*stats.done/stats.lessons));course.put("completed",complete);}
        return new Totals(completed,lessonsDone,modulesDone);
    }
    private static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
}
