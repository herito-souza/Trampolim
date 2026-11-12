package com.trampolim.api.communication;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
public abstract class CommunicationApi {
 private final CommunicationStore store;
 protected CommunicationApi(CommunicationStore store){this.store=store;}
 @GetMapping("/preferences") public Map<String,Object> preferences(@AuthenticationPrincipal String email){return store.preferences(email);}
 @PutMapping("/preferences") public Map<String,Object> save(@AuthenticationPrincipal String email,@RequestBody CommunicationStore.Preferences input){return store.save(email,input);}
 @GetMapping public Map<String,Object> history(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return store.history(email,before,limit,false);}
 @GetMapping("/deliveries") public Map<String,Object> deliveries(@AuthenticationPrincipal String email,@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return store.history(email,before,limit,true);}
 @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)public void read(@AuthenticationPrincipal String email,@PathVariable long id){store.read(email,id);}
}
