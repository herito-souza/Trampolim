package com.trampolim.api.education;

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
@RequestMapping("/api/admin/capacita/legacy-migration")
@ConditionalOnProperty(name = "trampolim.datasource.capacita.enabled", havingValue = "true")
public class LegacyEducationMigrationController {
    private final LegacyEducationMigrationService migration;
    private final PlatformAccess platformAccess;

    public LegacyEducationMigrationController(LegacyEducationMigrationService migration, PlatformAccess platformAccess) {
        this.migration = migration;
        this.platformAccess = platformAccess;
    }

    @GetMapping("/preview")
    public LegacyEducationMigrationService.Preview preview(@AuthenticationPrincipal String email) {
        requireSuperAdmin(email);
        return migration.preview();
    }

    @PostMapping("/execute")
    public LegacyEducationMigrationService.MigrationResult execute(@AuthenticationPrincipal String email) {
        requireSuperAdmin(email);
        return migration.migrate();
    }

    private void requireSuperAdmin(String email) {
        if (!platformAccess.isSuperAdmin(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A migração é restrita à administração global da plataforma.");
    }
}
