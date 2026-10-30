package com.trampolim.api.career;
import java.util.*;import com.trampolim.api.radar.RadarBridge;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true") public class CareerOperationsController {
 private final CompanyCareerService companies;private final RadarBridge bridge;
 public CareerOperationsController(CompanyCareerService companies,RadarBridge bridge){this.companies=companies;this.bridge=bridge;}
 public record Token(String token){}
 @PostMapping("/api/company/career/portfolio/verify")public Object verify(@AuthenticationPrincipal String email,@RequestBody Token input){companies.member(email,true);if(input==null||input.token()==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);return bridge.convert(bridge.post("/integrations/career/portfolio/verify",new CareerPortfolioController.Verify(1,input.token(),email)),Map.class);}
}
