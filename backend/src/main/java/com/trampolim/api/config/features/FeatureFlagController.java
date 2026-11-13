package com.trampolim.api.config.features;

import com.trampolim.api.security.TrailTokenService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/features")
public class FeatureFlagController {
    private final TrailTokenService tokens;private final List<TrailFeatureFlags> stores;
    public FeatureFlagController(TrailTokenService tokens,ObjectProvider<TrailFeatureFlags> stores){this.tokens=tokens;this.stores=stores.orderedStream().toList();}
    @GetMapping public Map<String,Object> available(@AuthenticationPrincipal String email,@RequestHeader(name="Authorization",required=false)String authorization){
        if(email==null||authorization==null||!authorization.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String token=authorization.substring(7);for(var trail:TrailTokenService.Trail.values()){var decoded=tokens.verify(token,trail);if(decoded==null||!decoded.getSubject().equalsIgnoreCase(email))continue;
            String name=trail.name();var store=stores.stream().filter(s->s.trail().equals(name)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Esta trilha está indisponível."));
            return Map.of("trail",name.toLowerCase(java.util.Locale.ROOT),"features",store.availableFor(email,decoded.getClaim("uid").asLong()));}
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
