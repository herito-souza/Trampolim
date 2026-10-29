package com.trampolim.api.company;

import com.trampolim.api.security.PlatformAccess;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/empresas/legacy-migration")
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class LegacyCompanyMigrationController {
    private final LegacyCompanyMigrationService migration;
    private final PlatformAccess platformAccess;

    public LegacyCompanyMigrationController(LegacyCompanyMigrationService migration, PlatformAccess platformAccess) {
        this.migration = migration;
        this.platformAccess = platformAccess;
    }

    @GetMapping("/preview")
    public LegacyCompanyMigrationService.Preview preview(@AuthenticationPrincipal String email) {
        requireSuperAdmin(email);
        return migration.preview();
    }

    @PostMapping("/execute")
    public LegacyCompanyMigrationService.MigrationResult execute(@AuthenticationPrincipal String email) {
        requireSuperAdmin(email);
        return migration.migrate(email);
    }

    private void requireSuperAdmin(String email) {
        if (!platformAccess.isSuperAdmin(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A migração é restrita à administração global da plataforma.");
    }
}
