package com.trampolim.api.company;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/empresas/account")
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class CompanyAccountController {
    private final JdbcTemplate companies;
    public CompanyAccountController(@Qualifier("empresasJdbcTemplate") JdbcTemplate companies) { this.companies = companies; }
    @GetMapping("/export")
    public Map<String,Object> exportAccount(@AuthenticationPrincipal String email) {
        if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var rows = companies.queryForList("SELECT id,email,full_name,email_verified,account_status,terms_accepted_at,created_at,last_login_at FROM company_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE'", email);
        if (rows.size() != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada.");
        var account = rows.get(0);
        var memberships = companies.queryForList("SELECT company_id,role FROM company_members WHERE user_id=? ORDER BY company_id", account.get("id"));
        var organizations = companies.queryForList("SELECT organization_id,role,created_at FROM enterprise_organization_members WHERE LOWER(email)=LOWER(?) ORDER BY organization_id,role", email);
        return Map.of("exportedAt", Instant.now().toString(), "trail", "empresas", "account", account, "companyMemberships", memberships, "organizationMemberships", organizations);
    }
}
