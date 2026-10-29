package com.trampolim.api.company;

import com.trampolim.api.security.PlatformAccess;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/empresas")
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class CompanyVerificationController {
    private final JdbcTemplate empresas;
    private final PlatformAccess platformAccess;

    public CompanyVerificationController(@Qualifier("empresasJdbcTemplate") JdbcTemplate empresas, PlatformAccess platformAccess) {
        this.empresas = empresas;
        this.platformAccess = platformAccess;
    }

    public record VerificationInput(String status, String reason) {}
    public record CompanyReview(long id, String legalName, String tradeName, String cnpj, String city, String state,
            String verificationStatus, String createdAt) {}

    @GetMapping("/pending")
    public List<CompanyReview> pending(@AuthenticationPrincipal String email) {
        requireAdmin(email);
        return empresas.query("SELECT id,legal_name,trade_name,cnpj,city,state,verification_status,created_at FROM companies WHERE verification_status='PENDING' ORDER BY created_at",
                (rs, row) -> new CompanyReview(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getTimestamp(8).toInstant().toString()));
    }

    @PutMapping("/{companyId}/verification")
    @Transactional(transactionManager = "empresasTransactionManager")
    public Map<String, String> review(@AuthenticationPrincipal String email, @PathVariable long companyId,
            @RequestBody VerificationInput input) {
        requireAdmin(email);
        if (input == null || !List.of("APPROVED", "REJECTED", "SUSPENDED").contains(input.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de validação inválido.");
        String reason = input.reason() == null ? "" : input.reason().trim();
        if (reason.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A justificativa deve ter até 500 caracteres.");
        int updated = empresas.update("UPDATE companies SET verification_status=? WHERE id=?", input.status(), companyId);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada.");
        empresas.update("INSERT INTO company_audit_events(company_id,event_type,details) VALUES (?, 'COMPANY_VERIFICATION_CHANGED', jsonb_build_object('status', ?, 'reason', ?))",
                companyId, input.status(), reason);
        return Map.of("status", input.status());
    }

    private void requireAdmin(String email) {
        if (!platformAccess.isSuperAdmin(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito à administração da plataforma.");
    }
}
