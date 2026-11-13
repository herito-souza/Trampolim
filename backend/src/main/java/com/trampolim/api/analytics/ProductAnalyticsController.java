package com.trampolim.api.analytics;

import com.trampolim.api.security.PlatformAccess;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Administrative, aggregate-only product analytics endpoint. */
@RestController
@RequestMapping("/api/admin/product-analytics")
public class ProductAnalyticsController {
    private final ProductAnalyticsService analytics;
    private final PlatformAccess access;
    public ProductAnalyticsController(ProductAnalyticsService analytics, PlatformAccess access) {
        this.analytics=analytics; this.access=access;
    }

    @GetMapping("/{trail}")
    public Map<String,Object> report(@AuthenticationPrincipal String email,
            @PathVariable String trail,
            @RequestParam String fromMonth,
            @RequestParam String toMonth,
            @RequestParam(defaultValue="") String state,
            @RequestParam(defaultValue="") String municipalityCode) {
        if(!access.isSuperAdmin(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso restrito à administração da plataforma.");
        return analytics.report(trail,fromMonth,toMonth,state,municipalityCode);
    }
}
