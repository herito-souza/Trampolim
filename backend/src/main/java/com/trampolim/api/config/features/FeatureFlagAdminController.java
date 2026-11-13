package com.trampolim.api.config.features;

import com.trampolim.api.security.PlatformAccess;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/features")
public class FeatureFlagAdminController {
    private final PlatformAccess access;
    private final List<TrailFeatureFlags> stores;
    public FeatureFlagAdminController(PlatformAccess access,ObjectProvider<TrailFeatureFlags> stores){this.access=access;this.stores=stores.orderedStream().toList();}
    @GetMapping("/{trail}") public List<FeatureFlag> list(@AuthenticationPrincipal String email,@PathVariable String trail){admin(email);return store(trail).list();}
    @GetMapping("/{trail}/{key}/audit") public List<java.util.Map<String,Object>> audit(@AuthenticationPrincipal String email,@PathVariable String trail,@PathVariable String key){admin(email);return store(trail).audit(key);}
    @PutMapping("/{trail}/{key}") public FeatureFlag update(@AuthenticationPrincipal String email,@PathVariable String trail,@PathVariable String key,@RequestBody TrailFeatureFlags.Change change){admin(email);return store(trail).update(key,email,change);}
    private void admin(String email){if(!access.isSuperAdmin(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso restrito à administração da plataforma.");}
    private TrailFeatureFlags store(String trail){return stores.stream().filter(s->s.trail().equalsIgnoreCase(trail)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Trilha indisponível."));}
}
