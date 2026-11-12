package com.trampolim.api.fairs;
import java.util.Map;import org.springframework.web.bind.annotation.*;import org.springframework.beans.factory.*;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;import com.trampolim.api.radar.RadarBridge;
@RestController public class FairCountsController {
 private final FairRegistrationStore candidates;private final ObjectProvider<FairRegistrationStore> students;private final RadarBridge bridge;
 public FairCountsController(@Qualifier("candidateFairRegistrations")FairRegistrationStore candidates,@Qualifier("studentFairRegistrations")ObjectProvider<FairRegistrationStore> students,RadarBridge bridge){this.candidates=candidates;this.students=students;this.bridge=bridge;}
 @PostMapping("/api/integrations/job-fairs/opportunities/counts")public Map<String,Object> candidates(@RequestHeader(value="X-Trampolim-Bridge",required=false)String key,@RequestBody FairRules.CountQuery input){bridge.authorize(key);return candidates.counts(input);}
 @PostMapping("/api/integrations/job-fairs/learning/counts")public Map<String,Object> students(@RequestHeader(value="X-Trampolim-Bridge",required=false)String key,@RequestBody FairRules.CountQuery input){bridge.authorize(key);var store=students.getIfAvailable();if(store==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);return store.counts(input);}
}
