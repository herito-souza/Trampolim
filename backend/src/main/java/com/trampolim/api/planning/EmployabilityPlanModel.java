package com.trampolim.api.planning;
import java.util.List;
import java.util.UUID;
public final class EmployabilityPlanModel {
 private EmployabilityPlanModel(){}
 public record SkillGoal(String skillCode,int level,Boolean requireEvidence){}
 public record JobRef(String source,long id){}
 public record Objectives(String occupation,String area,String city,String modality,List<SkillGoal> skills,List<JobRef> jobs){}
 public record Update(Objectives objectives,Long expectedRevision){}
 public record Refresh(Long expectedRevision,UUID requestKey){}
 public record Progress(String state,Long expectedRevision){}
}
