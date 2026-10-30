package com.trampolim.api.future;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import static com.trampolim.api.future.FutureModel.*;
@RestController
public class FutureTalentController {
 private final FutureTalentService service;
 public FutureTalentController(FutureTalentService service){this.service=service;}
 @GetMapping("/api/future/demands")public Map<String,Object> browse(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String region,@RequestParam(defaultValue="")String modality){return service.browse(email,before,limit,q,region,modality);}
 @GetMapping("/api/future/interests")public List<Map<String,Object>> interests(@AuthenticationPrincipal String email){return service.interests(email);}
 @PutMapping("/api/future/demands/{id}/interest")@ResponseStatus(HttpStatus.NO_CONTENT)public void interest(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Interest input){service.interest(email,id,input);}
 @DeleteMapping("/api/future/demands/{id}/interest")@ResponseStatus(HttpStatus.NO_CONTENT)public void withdraw(@AuthenticationPrincipal String email,@PathVariable long id){service.withdraw(email,id);}
 @GetMapping("/api/future/demands/{id}/preparation")public Map<String,Object> preparation(@AuthenticationPrincipal String email,@PathVariable long id){return service.prepare(email,id);}
 @GetMapping("/api/future/demands/{id}/plans")public List<Map<String,Object>> plans(@AuthenticationPrincipal String email,@PathVariable long id){return service.plans(email,id);}
 @PutMapping("/api/future/demands/{id}/plans")@ResponseStatus(HttpStatus.NO_CONTENT)public void plan(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Plan input){service.plan(email,id,input);}
 @GetMapping("/api/future/notifications")public Map<String,Object> notifications(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return service.notifications(email,before,limit);}
 @PutMapping("/api/future/notifications/{id}/read")@ResponseStatus(HttpStatus.NO_CONTENT)public void read(@AuthenticationPrincipal String email,@PathVariable long id){service.read(email,id);}
 @GetMapping("/api/future/vacancies/{id}")public Map<String,Object> vacancy(@AuthenticationPrincipal String email,@PathVariable long id){return service.vacancy(email,id);}
 @PostMapping("/api/integrations/future/summary")public Map<String,Object> summary(@RequestHeader(name="X-Trampolim-Bridge",required=false)String key,@RequestBody SummaryRequest input){return service.summary(key,input);}
}
