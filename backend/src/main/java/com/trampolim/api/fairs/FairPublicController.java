package com.trampolim.api.fairs;
import java.util.Map;import org.springframework.web.bind.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/public/job-fairs") @ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class FairPublicController {
 private final FairService service;public FairPublicController(FairService service){this.service=service;}
 @GetMapping public Map<String,Object> list(@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String region,@RequestParam(defaultValue="")String municipalityCode,@RequestParam(defaultValue="")String state,@RequestParam(defaultValue="")String modality,@RequestParam(defaultValue="")String from,@RequestParam(defaultValue="")String to,@RequestParam(defaultValue="false")boolean includePast){return service.publicList(before,limit,q,region,municipalityCode,state,modality,from,to,includePast);}
 @GetMapping("/{id}")public Map<String,Object> event(@PathVariable long id){return service.publicEvent(id);}
 @GetMapping("/{id}/companies")public Map<String,Object> companies(@PathVariable long id,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return service.publicCompanies(id,before,limit);}
 @GetMapping("/{id}/jobs")public Map<String,Object> jobs(@PathVariable long id,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="")String area,@RequestParam(defaultValue="")String modality){return service.publicJobs(id,before,limit,area,modality);}
 @GetMapping("/{id}/courses")public Map<String,Object> courses(@PathVariable long id,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return service.publicCourses(id,before,limit);}
 @GetMapping("/{id}/sessions")public Map<String,Object> sessions(@PathVariable long id,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return service.sessions(null,0,id,before,limit,true);}
}
