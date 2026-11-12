package com.trampolim.api.fairs;
import java.util.Map;import static com.trampolim.api.fairs.FairRules.*;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.http.HttpStatus;
public abstract class FairRegistrationApi {
 private final FairRegistrationStore store;protected FairRegistrationApi(FairRegistrationStore store){this.store=store;}
 @GetMapping("/{id}/registration")public Map<String,Object> get(@AuthenticationPrincipal String email,@PathVariable long id){return store.get(email,id);}
 @PutMapping("/{id}/registration")public Map<String,Object> join(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Registration input){return store.join(email,id,input);}
 @DeleteMapping("/{id}/registration")@ResponseStatus(HttpStatus.NO_CONTENT)public void withdraw(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam long expectedRevision){store.withdraw(email,id,expectedRevision);}
 @GetMapping("/registrations")public Map<String,Object> list(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return store.list(email,before,limit);}
}
