package com.trampolim.api.observatory;
import java.util.*;import com.trampolim.api.radar.RadarBridge;import com.trampolim.api.education.LearningAccess;
import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
@RestController @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LearningObservatoryController {
 private final LearningObservatory service;private final RadarBridge bridge;private final LearningAccess access;
 public LearningObservatoryController(LearningObservatory service,RadarBridge bridge,LearningAccess access){this.service=service;this.bridge=bridge;this.access=access;}
 @PostMapping("/api/integrations/career/observatory/v2/learning")public Map<String,Object> read(@RequestHeader(value="X-Trampolim-Bridge",required=false)String key,@RequestBody ObservatoryController.LearningQuery p){bridge.authorize(key);if(p==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return service.indicators(p.fromMonth(),p.toMonth(),p.state());}
 @PostMapping("/api/integrations/career/observatory/v2/learning/refresh")@ResponseStatus(HttpStatus.ACCEPTED)public Map<String,Object> refresh(@RequestHeader(value="X-Trampolim-Bridge",required=false)String key){bridge.authorize(key);return service.enqueue();}
 @PostMapping("/api/capacita/learning/admin/observatory/refresh")@ResponseStatus(HttpStatus.ACCEPTED)public Map<String,Object> refreshOwn(@AuthenticationPrincipal String email){access.admin(email);return service.enqueue();}
 @GetMapping("/api/capacita/learning/admin/observatory/status")public Map<String,Object> status(@AuthenticationPrincipal String email){access.admin(email);return service.health();}
}
