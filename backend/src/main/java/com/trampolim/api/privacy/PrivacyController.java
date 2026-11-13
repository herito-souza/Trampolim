package com.trampolim.api.privacy;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping({"/api/profile/privacy","/api/empresas/account/privacy","/api/capacita/account/privacy"})
public class PrivacyController {
 private final Map<String,PrivacyStore> stores=new HashMap<>();
 public PrivacyController(List<PrivacyStore> stores){stores.forEach(s->this.stores.put(s.trail,s));}
 private PrivacyStore store(HttpServletRequest request){String path=request.getRequestURI();String trail=path.startsWith("/api/empresas/")?"empresas":path.startsWith("/api/capacita/")?"capacita":"oportunidades";var store=stores.get(trail);if(store==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);return store;}
 public record Input(UUID clientKey,boolean confirmed){}
 public record Revision(long expectedRevision){}
 @GetMapping("/requests")public List<Map<String,Object>> list(@AuthenticationPrincipal String email,HttpServletRequest req,@RequestParam(defaultValue="0")int page){return store(req).list(email,page);}
 @PostMapping("/requests")public Map<String,Object> request(@AuthenticationPrincipal String email,HttpServletRequest req,@RequestBody Input input){if(input==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return store(req).request(email,input.clientKey(),input.confirmed());}
 @PostMapping("/requests/{id}/cancel")public Map<String,Object> cancel(@AuthenticationPrincipal String email,HttpServletRequest req,@PathVariable UUID id,@RequestBody Revision revision){if(revision==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return store(req).cancel(email,id,revision.expectedRevision());}
}
