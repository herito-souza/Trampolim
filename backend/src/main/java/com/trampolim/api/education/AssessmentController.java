package com.trampolim.api.education;

import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/capacita/learning")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class AssessmentController {
    private final AssessmentService service;private final LearningPerformanceService performance;
    public AssessmentController(AssessmentService service,LearningPerformanceService performance){this.service=service;this.performance=performance;}
    public record Answers(Map<String,String> answers){}
    public record Consent(boolean consent){}
    public record Accommodation(String studentEmail,java.time.Instant extendedDueAt){}
    @PutMapping("/assessments/{id}/accommodations")@ResponseStatus(HttpStatus.NO_CONTENT)public void accommodate(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Accommodation input){service.accommodate(email,id,input);}
    @GetMapping("/assessments")public List<Map<String,Object>> list(@AuthenticationPrincipal String email,@RequestParam(defaultValue="false")boolean manage,@RequestParam(required=false)Long course,@RequestParam(required=false)Long module){return service.list(email,manage,course,module);}
    @PostMapping("/assessments")public Map<String,Object> create(@AuthenticationPrincipal String email,@RequestBody AssessmentService.Input input){return service.create(email,input);}
    @GetMapping("/assessments/{id}")public Map<String,Object> detail(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="false")boolean manage){return service.detail(email,id,manage);}
    @PutMapping("/assessments/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void edit(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody AssessmentService.Input input){service.edit(email,id,input);}
    @PostMapping("/assessments/{id}/attempts")public Map<String,Object> start(@AuthenticationPrincipal String email,@PathVariable long id){return service.start(email,id);}
    @GetMapping("/assessment-attempts")public List<Map<String,Object>> history(@AuthenticationPrincipal String email){return service.history(email);}
    @GetMapping("/assessment-attempts/{id}")public Map<String,Object> attempt(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestParam(defaultValue="false")boolean manage){return service.attempt(email,id,manage);}
    @PutMapping("/assessment-attempts/{id}/draft")@ResponseStatus(HttpStatus.NO_CONTENT)public void draft(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody Answers input){service.draft(email,id,input.answers());}
    @PostMapping("/assessment-attempts/{id}/submit")public Map<String,Object> submit(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody AssessmentService.Submission input){return service.submit(email,id,input);}
    @GetMapping("/assessments/{id}/results")public List<Map<String,Object>> report(@AuthenticationPrincipal String email,@PathVariable long id){return service.report(email,id);}
    @GetMapping("/performance")public Map<String,Object> dashboard(@AuthenticationPrincipal String email){return performance.dashboard(email);}
    @GetMapping("/competencies")public List<Map<String,Object>> competencies(@AuthenticationPrincipal String email){return performance.competencies(email);}
    @PutMapping("/competencies/{id}/presentation")@ResponseStatus(HttpStatus.NO_CONTENT)public void present(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody AssessmentService.Presentation input){performance.present(email,id,input);}
    @PostMapping("/competencies/{id}/share")public Map<String,Object> share(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody Consent input){return performance.share(email,id,input.consent());}
    @DeleteMapping("/competencies/{id}/share")@ResponseStatus(HttpStatus.NO_CONTENT)public void revoke(@AuthenticationPrincipal String email,@PathVariable UUID id){performance.revoke(email,id);}
    @GetMapping("/competencies/shared/{token}")public ResponseEntity<Map<String,Object>> shared(@PathVariable String token){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer").body(performance.shared(token));}
    @PostMapping("/competencies/export")public Map<String,Object> export(@AuthenticationPrincipal String email,@RequestBody Consent input){return performance.export(email,input.consent());}
}
