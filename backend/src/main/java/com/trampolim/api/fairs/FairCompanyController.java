package com.trampolim.api.fairs;
import java.util.Map;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/company/job-fairs") @ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class FairCompanyController {
 private final FairService service;public FairCompanyController(FairService service){this.service=service;}
 @GetMapping("/invitations")public Map<String,Object> invites(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return service.companyInvites(email,before,limit);}
 @PutMapping("/{id}/participation")public Map<String,Object> decide(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody FairRules.CompanyDecision input){return service.companyDecision(email,id,input);}
}
