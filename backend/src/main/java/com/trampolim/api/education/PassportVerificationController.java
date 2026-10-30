package com.trampolim.api.education;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class PassportVerificationController {
 private final com.trampolim.api.security.BoundedRateLimiter limiter=new com.trampolim.api.security.BoundedRateLimiter();
 private final PassportService passport;public PassportVerificationController(PassportService passport){this.passport=passport;}
 @GetMapping("/api/public/capacita/passport/verify/{id}")public ResponseEntity<Map<String,Object>> verify(@PathVariable UUID id,jakarta.servlet.http.HttpServletRequest request){if(!limiter.allow(request.getRemoteAddr(),60,java.time.Duration.ofMinutes(1)))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Aguarde antes de consultar novamente.");return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(passport.verification(id));}
 @PostMapping("/api/integrations/capacita/passport/verify")public Map<String,Object> share(@RequestHeader(name="X-Trampolim-Bridge",required=false)String key,@RequestBody PassportService.Verify input){return passport.verify(key,input);}
}
