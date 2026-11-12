package com.trampolim.api.education;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class ProfessionalPassportController {
 private final ProfessionalPassportService service;public ProfessionalPassportController(ProfessionalPassportService service){this.service=service;}
 @PostMapping("/api/capacita/learning/passport/professional")public Map<String,Object> claim(@AuthenticationPrincipal String email,@RequestBody ProfessionalPassportService.Import p){return service.claim(email,p);}
 @GetMapping("/api/capacita/learning/passport/professional")public Map<String,Object> list(@AuthenticationPrincipal String email,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){return service.list(email,offset,limit);}
 @GetMapping("/api/capacita/learning/passport/professional/{id}")public Map<String,Object> detail(@AuthenticationPrincipal String email,@PathVariable UUID id){return service.detail(email,id);}
 @DeleteMapping("/api/capacita/learning/passport/professional/{id}")public void revoke(@AuthenticationPrincipal String email,@PathVariable UUID id){service.revoke(email,id);}
 @PostMapping("/api/capacita/learning/passport/professional-shares")public Map<String,Object> share(@AuthenticationPrincipal String email,@RequestBody ProfessionalPassportService.Share p){return service.share(email,p);}
 @GetMapping("/api/capacita/learning/passport/professional-shares")public List<Map<String,Object>> shares(@AuthenticationPrincipal String email){return service.shares(email);}
 @DeleteMapping("/api/capacita/learning/passport/professional-shares/{id}")public void revokeShare(@AuthenticationPrincipal String email,@PathVariable UUID id){service.revokeShare(email,id);}
 @PostMapping("/api/integrations/capacita/passport/professional/verify")public Map<String,Object> verify(@RequestHeader(name="X-Trampolim-Bridge",required=false)String key,@RequestBody ProfessionalPassportService.Verify p){return service.verify(key,p);}
}
