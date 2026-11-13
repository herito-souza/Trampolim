package com.trampolim.api.trust;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;
public abstract class ReportApi {
 private final ListingReportGateway service;protected ReportApi(ListingReportGateway service){this.service=service;}
 @PostMapping("/listings/{id}/reports")public Map<String,Object> submit(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody ListingReportGateway.Input p){return service.submit(email,id,p);}
 @GetMapping("/reports")public List<Map<String,Object>> list(@AuthenticationPrincipal String email){return service.list(email);}
 @GetMapping("/reports/{receipt}")public Map<String,Object> own(@AuthenticationPrincipal String email,@PathVariable UUID receipt){return service.own(email,receipt);}
 @DeleteMapping("/reports/{receipt}")public void withdraw(@AuthenticationPrincipal String email,@PathVariable UUID receipt){service.withdraw(email,receipt);}
 @PostMapping("/reports/{receipt}/retry")public Map<String,Object> retry(@AuthenticationPrincipal String email,@PathVariable UUID receipt){return service.retry(email,receipt);}
}
