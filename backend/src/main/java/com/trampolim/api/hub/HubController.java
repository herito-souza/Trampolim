package com.trampolim.api.hub;
import java.util.*;import java.time.*;
import static com.trampolim.api.hub.HubRules.*;
import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.http.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/empresas/hub/organizations/{org}")
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class HubController {
 private final HubService service;private final HubAccess access;
 public HubController(HubService service,HubAccess access){this.service=service;this.access=access;}
 @GetMapping("/profile")public Map<String,Object> profile(@AuthenticationPrincipal String email,@PathVariable long org){boolean manager=access.require(email,org,false);var result=new LinkedHashMap<>(service.profile(org));result.put("canManage",manager);return result;}
 @PutMapping("/profile")public Map<String,Object> profile(@AuthenticationPrincipal String email,@PathVariable long org,@RequestBody Profile p){access.require(email,org,true);if(p!=null&&p.published())access.additionalWrite(org);var result=new LinkedHashMap<>(service.saveProfile(org,email,p));result.put("canManage",true);return result;}
 @PostMapping("/logo")public Map<String,Object> logo(@AuthenticationPrincipal String email,@PathVariable long org,@RequestParam MultipartFile file){access.require(email,org,true);access.additionalWrite(org);return service.saveLogo(org,email,file);}
 @GetMapping("/logo")public ResponseEntity<byte[]> logo(@AuthenticationPrincipal String email,@PathVariable long org){access.require(email,org,false);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").body(service.logo(org));}
 @GetMapping("/usage")public Map<String,Object> usage(@AuthenticationPrincipal String email,@PathVariable long org){access.require(email,org,false);return service.usage(org);}
 @GetMapping("/contracts")public List<Map<String,Object>> contracts(@AuthenticationPrincipal String email,@PathVariable long org){access.require(email,org,true);return service.contracts(org);}
 @GetMapping("/companies")public Map<String,Object> companies(@AuthenticationPrincipal String email,@PathVariable long org,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){access.require(email,org,false);return service.companies(org,before,limit);}
 @GetMapping("/programs")public Map<String,Object> programs(@AuthenticationPrincipal String email,@PathVariable long org,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){access.require(email,org,false);return service.programs(org,before,limit);}
 @GetMapping("/programs/{id}")public Map<String,Object> program(@AuthenticationPrincipal String email,@PathVariable long org,@PathVariable long id){access.require(email,org,false);return service.program(org,id);}
 @PostMapping("/programs")public Map<String,Object> create(@AuthenticationPrincipal String email,@PathVariable long org,@RequestBody Program p){access.require(email,org,true);access.additionalWrite(org);return service.saveProgram(org,0,email,p);}
 @PutMapping("/programs/{id}")public Map<String,Object> update(@AuthenticationPrincipal String email,@PathVariable long org,@PathVariable long id,@RequestBody Program p){access.require(email,org,true);if(p!=null&&!"ARCHIVED".equals(p.status()))access.additionalWrite(org);return service.saveProgram(org,id,email,p);}
 @GetMapping("/campaigns")public Map<String,Object> campaigns(@AuthenticationPrincipal String email,@PathVariable long org,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){access.require(email,org,false);return service.campaigns(org,before,limit);}
 @PostMapping("/campaigns")public Map<String,Object> campaign(@AuthenticationPrincipal String email,@PathVariable long org,@RequestBody Campaign p){access.require(email,org,true);access.additionalWrite(org);return service.campaign(org,0,email,p);}
 @PutMapping("/campaigns/{id}")public Map<String,Object> campaign(@AuthenticationPrincipal String email,@PathVariable long org,@PathVariable long id,@RequestBody Campaign p){access.require(email,org,true);if(p!=null&&!"ARCHIVED".equals(p.status()))access.additionalWrite(org);return service.campaign(org,id,email,p);}
 @GetMapping("/dashboard")public Map<String,Object> dashboard(@AuthenticationPrincipal String email,@PathVariable long org,@RequestParam String from,@RequestParam String to,@RequestParam(required=false)Long company){access.require(email,org,false);return service.dashboard(org,from,to,company);}
}
