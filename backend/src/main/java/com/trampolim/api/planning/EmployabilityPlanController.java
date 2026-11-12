package com.trampolim.api.planning;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import static com.trampolim.api.planning.EmployabilityPlanModel.*;
@RestController @RequestMapping("/api/employability-plan")
public class EmployabilityPlanController {
 private final EmployabilityPlanService service;
 public EmployabilityPlanController(EmployabilityPlanService service){this.service=service;}
 @GetMapping public Map<String,Object> get(@AuthenticationPrincipal String email){return service.get(email);}
 @PutMapping("/objectives") public Map<String,Object> update(@AuthenticationPrincipal String email,@RequestBody Update input){return service.update(email,input);}
 @PostMapping("/refresh") public Map<String,Object> refresh(@AuthenticationPrincipal String email,@RequestBody Refresh input){return service.refresh(email,input);}
 @PutMapping("/steps/{key}") public Map<String,Object> progress(@AuthenticationPrincipal String email,@PathVariable String key,@RequestBody Progress input){return service.progress(email,key,input);}
 @GetMapping("/generations") public Map<String,Object> history(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="10")int limit){return service.history(email,before,limit);}
 @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal String email,@RequestParam long expectedRevision){service.delete(email,expectedRevision);}
}
