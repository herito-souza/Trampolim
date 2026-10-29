package com.trampolim.api.security;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps the configured bootstrap administrators while supporting database-managed platform roles. */
@Component
@RequiredArgsConstructor
public class PlatformAccess {
    private final JdbcTemplate jdbc;
    @Value("${app.admin.emails:}") private String bootstrapAdminEmails;

    public boolean isSuperAdmin(String email) {
        if (email == null || email.isBlank()) return false;
        boolean bootstrap = Arrays.stream(bootstrapAdminEmails.split(","))
                .map(String::trim).anyMatch(configured -> configured.equalsIgnoreCase(email));
        if (bootstrap) return true;
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM platform_roles pr JOIN app_users u ON u.id=pr.user_id WHERE LOWER(u.email)=LOWER(?) AND pr.role='SUPER_ADMIN'", Integer.class, email);
        return count != null && count > 0;
    }
}
