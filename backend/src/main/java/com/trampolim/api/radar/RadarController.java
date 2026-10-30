package com.trampolim.api.radar;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import static com.trampolim.api.radar.RadarModel.*;
@RestController
public class RadarController {
 private final RadarService service;
 public RadarController(RadarService service){this.service=service;}
 @GetMapping("/api/radar/profile")public Map<String,Object> profile(@AuthenticationPrincipal String email){return service.profile(email);}
 @PutMapping("/api/radar/preferences")@ResponseStatus(HttpStatus.NO_CONTENT)public void preferences(@AuthenticationPrincipal String email,@RequestBody RadarService.Preferences input){service.preferences(email,input);}
 @PutMapping("/api/radar/skills")@ResponseStatus(HttpStatus.NO_CONTENT)public void declare(@AuthenticationPrincipal String email,@RequestBody RadarService.Declaration input){service.declare(email,input);}
 @DeleteMapping("/api/radar/skills/{code}")@ResponseStatus(HttpStatus.NO_CONTENT)public void remove(@AuthenticationPrincipal String email,@PathVariable String code){service.removeDeclaration(email,code);}
 @DeleteMapping("/api/radar/evidence/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void removeEvidence(@AuthenticationPrincipal String email,@PathVariable UUID id){service.removeEvidence(email,id);}
 @PostMapping("/api/radar/link-challenges")public Map<String,Object> challenge(@AuthenticationPrincipal String email){return service.challenge(email);}
 @GetMapping("/api/radar/jobs")public Map<String,Object> jobs(@AuthenticationPrincipal String email,@RequestParam(defaultValue="company")String source,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="")String q){return service.jobs(email,source,before,limit,q);}
 @GetMapping("/api/radar/matches/{source}/{id}")public Map<String,Object> match(@AuthenticationPrincipal String email,@PathVariable String source,@PathVariable long id){return service.match(email,source,id);}
 @GetMapping("/api/radar/policy")public Policy policy(){return service.policy();}
 @PutMapping("/api/radar/admin/policy")@ResponseStatus(HttpStatus.NO_CONTENT)public void policy(@AuthenticationPrincipal String email,@RequestBody Policy input){service.policy(email,input);}
 @GetMapping("/api/radar/admin/catalog/{id}/requirements")public Requirements requirements(@AuthenticationPrincipal String email,@PathVariable long id){return service.catalogRequirements(email,id);}
 @PutMapping("/api/radar/admin/catalog/{id}/requirements")@ResponseStatus(HttpStatus.NO_CONTENT)public void requirements(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Requirements input){service.catalogRequirements(email,id,input);}
 @PostMapping("/api/integrations/radar/award")public Map<String,Object> receive(@RequestHeader(name="X-Trampolim-Bridge",required=false)String key,@RequestBody RadarService.Claim input){return service.accept(key,input);}
}
