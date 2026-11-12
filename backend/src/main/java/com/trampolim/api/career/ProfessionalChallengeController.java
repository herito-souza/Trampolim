package com.trampolim.api.career;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class ProfessionalChallengeController {
 private final ProfessionalChallengeService service;public ProfessionalChallengeController(ProfessionalChallengeService service){this.service=service;}
 public record Suspension(int expectedRevision,String reason){}
 @PostMapping("/api/company/challenges/{id}/proposal")public Map<String,Object> propose(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody ProfessionalChallengeService.Proposal p){return service.propose(email,id,p);}
 @GetMapping("/api/company/challenges/{id}/indicators")public Map<String,Object> indicators(@AuthenticationPrincipal String email,@PathVariable long id){return service.indicators(email,id);}
 @GetMapping("/api/admin/professional-challenges")public Map<String,Object> queue(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return service.queue(email,before,limit);}
 @GetMapping("/api/admin/professional-challenges/{id}")public Map<String,Object> inspect(@AuthenticationPrincipal String email,@PathVariable long id){return service.inspect(email,id);}
 @PostMapping("/api/admin/professional-challenges/{id}/decision")public Map<String,Object> decide(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody ProfessionalChallengeService.Decision p){return service.decide(email,id,p);}
 @PostMapping("/api/admin/professional-challenges/{id}/suspend")public void suspend(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Suspension p){if(p==null)com.trampolim.api.radar.RadarModel.bad("Informe justificativa e revisão.");service.suspend(email,id,p.expectedRevision(),p.reason());}
 @PostMapping("/api/integrations/career/challenges/evidence")public Map<String,Object> evidence(@RequestHeader(name="X-Trampolim-Bridge",required=false)String key,@RequestBody CompanyCareerService.Receipt p){return service.evidence(key,p);}
 public record Revocation(String reason){}
 @PostMapping("/api/company/challenges/{id}/submissions/{receipt}/review/revoke")public void revokeReview(@AuthenticationPrincipal String email,@PathVariable long id,@PathVariable UUID receipt,@RequestBody Revocation p){if(p==null)com.trampolim.api.radar.RadarModel.bad("Informe justificativa.");service.revokeReview(email,id,receipt,p.reason());}
}
