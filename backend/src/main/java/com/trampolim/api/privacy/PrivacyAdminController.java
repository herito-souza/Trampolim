package com.trampolim.api.privacy;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.trampolim.api.security.PlatformAccess;
/** Existing global administration boundary; company/student tokens cannot authenticate here. */
@RestController @RequestMapping("/api/admin/privacy")
public class PrivacyAdminController {
 private final PlatformAccess access;private final JdbcTemplate identities;private final Map<String,PrivacyStore> stores=new HashMap<>();
 public PrivacyAdminController(PlatformAccess access,@Qualifier("jdbcTemplate")JdbcTemplate identities,List<PrivacyStore> stores){this.access=access;this.identities=identities;stores.forEach(s->this.stores.put(s.trail,s));}
 private PrivacyStore authorized(String email,String trail){if(!access.isSuperAdmin(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN);var store=stores.get(trail);if(store==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);return store;}
 @GetMapping("/{trail}/requests")public List<Map<String,Object>> queue(@AuthenticationPrincipal String email,@PathVariable String trail,@RequestParam(defaultValue="0")int page){return authorized(email,trail).queue(page);}
 @PutMapping("/{trail}/requests/{id}")public Map<String,Object> review(@AuthenticationPrincipal String email,@PathVariable String trail,@PathVariable UUID id,@RequestBody PrivacyStore.Review input){var store=authorized(email,trail);Long actor=identities.queryForObject("SELECT id FROM app_users WHERE LOWER(email)=LOWER(?)",Long.class,email);return store.review(id,input,"platform-user:"+actor);}
 @GetMapping("/{trail}/requests/{id}/audit")public List<Map<String,Object>> audit(@AuthenticationPrincipal String email,@PathVariable String trail,@PathVariable UUID id,@RequestParam(defaultValue="0")int page){return authorized(email,trail).audit(id,page);}
 @GetMapping("/{trail}/disposal-preview")public Map<String,Object> preview(@AuthenticationPrincipal String email,@PathVariable String trail,@RequestParam int days){return authorized(email,trail).disposalPreview(days);}
}
