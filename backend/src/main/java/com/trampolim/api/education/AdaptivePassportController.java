package com.trampolim.api.education;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.http.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/capacita/learning") @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class AdaptivePassportController {
 private final AdaptiveService adaptive;private final PassportService passport;
 public AdaptivePassportController(AdaptiveService adaptive,PassportService passport){this.adaptive=adaptive;this.passport=passport;}
 @GetMapping("/adaptive/goals")public List<Map<String,Object>> goals(@AuthenticationPrincipal String email){return adaptive.goals(email);}
 @PostMapping("/adaptive/goals")public Map<String,Object> create(@AuthenticationPrincipal String email,@RequestBody AdaptiveService.Goal input){return adaptive.save(email,null,input);}
 @PutMapping("/adaptive/goals/{id}")public Map<String,Object> edit(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody AdaptiveService.Goal input){return adaptive.save(email,id,input);}
 @DeleteMapping("/adaptive/goals/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@AuthenticationPrincipal String email,@PathVariable UUID id){adaptive.delete(email,id);}
 @GetMapping("/adaptive/goals/{id}/plan")public Map<String,Object> plan(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestParam(defaultValue="false")boolean refresh){return adaptive.plan(email,id,refresh);}
 @GetMapping("/adaptive/modules/{id}/policy")public Map<String,Object> policy(@AuthenticationPrincipal String email,@PathVariable long id){return adaptive.policy(email,id);}
 @PutMapping("/adaptive/modules/{id}/policy")@ResponseStatus(HttpStatus.NO_CONTENT)public void policy(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody AdaptiveService.ModulePolicy input){adaptive.policy(email,id,input);}
 @GetMapping("/passport/credentials")public Map<String,Object> credentials(@AuthenticationPrincipal String email,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return passport.credentials(email,offset,limit);}
 @GetMapping("/passport/issued")public List<Map<String,Object>> issued(@AuthenticationPrincipal String email,@RequestParam long course){return passport.issued(email,course);}
 @PutMapping("/passport/credentials/{id}/revoke")@ResponseStatus(HttpStatus.NO_CONTENT)public void revoke(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody PassportService.Revocation input){passport.revokeCredential(email,id,input);}
 @GetMapping("/passport/shares")public List<Map<String,Object>> shares(@AuthenticationPrincipal String email){return passport.shares(email);}
 @PostMapping("/passport/shares")public Map<String,Object> share(@AuthenticationPrincipal String email,@RequestBody PassportService.Share input){return passport.share(email,input);}
 @DeleteMapping("/passport/shares/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void revokeShare(@AuthenticationPrincipal String email,@PathVariable UUID id){passport.revokeShare(email,id);}
 @GetMapping("/passport/credentials/{id}/verify")public Map<String,Object> ownVerify(@AuthenticationPrincipal String email,@PathVariable UUID id){adaptive.goals(email);return passport.verification(id);}
}
