package com.trampolim.api.organization;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.security.PlatformAccess;

@RestController
public class OrganizationController {
    private final JdbcTemplate jdbc;
    private final ObjectProvider<JdbcTemplate> empresasProvider;
    private final ObjectProvider<JdbcTemplate> capacitaProvider;
    private final PlatformAccess platformAccess;
    @Value("${trails.auth.legacy-organizations.enabled:false}") private boolean legacyOrganizationsEnabled;

    public OrganizationController(@Qualifier("jdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("empresasJdbcTemplate") ObjectProvider<JdbcTemplate> empresasProvider,
            @Qualifier("capacitaJdbcTemplate") ObjectProvider<JdbcTemplate> capacitaProvider,
            PlatformAccess platformAccess) {
        this.jdbc=jdbc;
        this.empresasProvider=empresasProvider;
        this.capacitaProvider=capacitaProvider;
        this.platformAccess=platformAccess;
    }

    public record Overview(long users, long verifiedUsers, long activeUsers30d, long companies, long associations,
            long associationLinks, long publishedJobs, long publishedInternships, long publishedLearningContents,
            long applications, long pendingPublications, long paidInternshipPayments, long internshipRevenueCents,
            List<MonthlyRevenue> revenueByMonth) {}
    public record MonthlyRevenue(String month, long paidPayments, long revenueCents) {}
    public record OrganizationView(long id, String name, String type, String status, Long legacyCompanyId, long members, long linkedCompanies, String createdAt) {}
    public record OrganizationInput(String name, String type) {}
    public record StatusInput(String status) {}
    public record CompanyLinkInput(long companyId, String status) {}
    public record LinkedCompany(long companyId, String name, String status) {}
    public record CompanyOption(long companyId, String name, String city, String state, String industry) {}
    public record MemberInput(String email, String role) {}
    public record OrganizationMember(long userId, String name, String email, String role, String createdAt) {}
    public record AssociationDashboard(long organizationId, String name, long companies, long activeCompanies,
            long openJobs, long openInternships, long applications, long publicLearningContents) {}
    public record PlatformRoleInput(String email, String role) {}
    public record PlatformAdminView(long userId, String name, String email, String grantedAt) {}

    @GetMapping("/api/admin/saas/overview")
    public Overview overview(@AuthenticationPrincipal String email) {
        requireSuperAdmin(email);
        JdbcTemplate empresas=required(empresasProvider,"Empresas");
        JdbcTemplate capacita=required(capacitaProvider,"Capacita");
        return new Overview(
                count("SELECT COUNT(*) FROM app_users WHERE account_type='PERSON'")+count(empresas,"SELECT COUNT(*) FROM company_users")+count(capacita,"SELECT COUNT(*) FROM student_users"),
                count("SELECT COUNT(*) FROM app_users WHERE account_type='PERSON' AND email_verified=TRUE")+count(empresas,"SELECT COUNT(*) FROM company_users WHERE email_verified=TRUE")+count(capacita,"SELECT COUNT(*) FROM student_users WHERE email_verified=TRUE"),
                count("SELECT COUNT(*) FROM app_users WHERE account_type='PERSON' AND last_login_at>=CURRENT_TIMESTAMP-INTERVAL '30 days'")+count(empresas,"SELECT COUNT(*) FROM company_users WHERE last_login_at>=CURRENT_TIMESTAMP-INTERVAL '30 days'")+count(capacita,"SELECT COUNT(*) FROM student_users WHERE last_login_at>=CURRENT_TIMESTAMP-INTERVAL '30 days'"),
                count(empresas,"SELECT COUNT(*) FROM companies"),
                count(empresas,"SELECT COUNT(*) FROM enterprise_organizations WHERE organization_type='ASSOCIATION'"),
                count(empresas,"SELECT COUNT(*) FROM enterprise_organization_companies WHERE relationship_type='ASSOCIATION_MEMBER' AND status='ACTIVE'"),
                count("SELECT COUNT(*) FROM catalog_items WHERE kind='job' AND active=TRUE AND moderation_status='APPROVED' AND (expires_at IS NULL OR expires_at>=CURRENT_DATE)")+count(empresas,"SELECT COUNT(*) FROM company_listings WHERE kind='job' AND is_internship=FALSE AND publication_status='PUBLISHED' AND (expires_at IS NULL OR expires_at>=CURRENT_DATE)"),
                count(empresas,"SELECT COUNT(*) FROM company_listings WHERE is_internship=TRUE AND payment_status='PAID' AND publication_status='PUBLISHED' AND (expires_at IS NULL OR expires_at>=CURRENT_DATE)"),
                count(capacita,"SELECT COUNT(*) FROM education_contents WHERE status='PUBLISHED'")+count("SELECT COUNT(*) FROM catalog_items WHERE kind='course' AND active=TRUE AND moderation_status='APPROVED' AND (expires_at IS NULL OR expires_at>=CURRENT_DATE)"),
                count(empresas,"SELECT COUNT(*) FROM company_applications")+count("SELECT COUNT(*) FROM job_applications"),
                count(empresas,"SELECT COUNT(*) FROM company_listings WHERE publication_status='AWAITING_PAYMENT'")+count("SELECT COUNT(*) FROM catalog_items WHERE moderation_status='PENDING'"),
                count(empresas,"SELECT COUNT(*) FROM company_listing_payments WHERE status='PAID'"),
                count(empresas,"SELECT COALESCE(SUM(amount_cents),0) FROM company_listing_payments WHERE status='PAID'"),
                empresas.query("SELECT to_char(months.month,'YYYY-MM') AS month, COUNT(p.id), COALESCE(SUM(p.amount_cents),0) FROM generate_series(date_trunc('month',CURRENT_DATE)-INTERVAL '5 months',date_trunc('month',CURRENT_DATE),INTERVAL '1 month') months(month) LEFT JOIN company_listing_payments p ON p.status='PAID' AND date_trunc('month',p.paid_at)=months.month GROUP BY months.month ORDER BY months.month", (rs, row) -> new MonthlyRevenue(rs.getString(1), rs.getLong(2), rs.getLong(3)))
        );
    }

    @GetMapping("/api/legacy/admin/saas/organizations")
    public List<OrganizationView> organizations(@AuthenticationPrincipal String email) {
        requireLegacyOrganizations();
        requireSuperAdmin(email);
        return organizationList("", new Object[]{});
    }

    @PostMapping("/api/legacy/admin/saas/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationView createOrganization(@AuthenticationPrincipal String email, @RequestBody OrganizationInput input) {
        requireLegacyOrganizations();
        requireSuperAdmin(email);
        String name = clean(input == null ? null : input.name());
        String type = clean(input == null ? null : input.type()).toUpperCase();
        if (name.isBlank() || name.length() > 180 || !Set.of("ASSOCIATION", "EDUCATIONAL", "QUALIFICATION").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome e tipo de organização válidos.");
        Long id = jdbc.queryForObject("INSERT INTO organizations(name,organization_type,status) VALUES (?,?, 'PENDING') RETURNING id", Long.class, name, type);
        audit(email, id, "ORGANIZATION_CREATED", "ORGANIZATION", id);
        return organizationById(id);
    }

    @PutMapping("/api/legacy/admin/saas/organizations/{id}/status")
    public OrganizationView updateOrganizationStatus(@AuthenticationPrincipal String email, @PathVariable long id, @RequestBody StatusInput input) {
        requireLegacyOrganizations();
        requireSuperAdmin(email);
        if (input == null || !Set.of("ACTIVE", "PENDING", "SUSPENDED").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de organização inválido.");
        if (jdbc.update("UPDATE organizations SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", input.status(), id) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada.");
        audit(email, id, "ORGANIZATION_STATUS_CHANGED", "ORGANIZATION", id);
        return organizationById(id);
    }

    @PostMapping("/api/legacy/organizations/{associationId}/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkCompany(@AuthenticationPrincipal String email, @PathVariable long associationId, @RequestBody CompanyLinkInput input) {
        requireLegacyOrganizations();
        boolean globalAdmin = platformAccess.isSuperAdmin(email);
        if (!globalAdmin) {
            requireOrganizationManager(email, associationId, "ASSOCIATION");
            if (input == null || !"PENDING".equals(input.status())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vínculos de empresas precisam ser aprovados pela administração global.");
        }
        if (input == null || input.companyId() <= 0 || !Set.of("ACTIVE", "PENDING").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vínculo inválido.");
        Integer association = jdbc.queryForObject("SELECT COUNT(*) FROM organizations WHERE id=? AND organization_type='ASSOCIATION'", Integer.class, associationId);
        Integer company = jdbc.queryForObject("SELECT COUNT(*) FROM organizations WHERE legacy_company_id=? AND organization_type='COMPANY'", Integer.class, input.companyId());
        if (association == null || association == 0 || company == null || company == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Associação ou empresa não encontrada.");
        jdbc.update("INSERT INTO organization_relationships(parent_organization_id,child_organization_id,relationship_type,status) SELECT ?,id,'ASSOCIATION_MEMBER',? FROM organizations WHERE legacy_company_id=? ON CONFLICT(parent_organization_id,child_organization_id,relationship_type) DO UPDATE SET status=EXCLUDED.status", associationId, input.status(), input.companyId());
        audit(email, associationId, "COMPANY_LINK_UPSERTED", "COMPANY", input.companyId());
    }

    @PostMapping("/api/legacy/organizations/{organizationId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(@AuthenticationPrincipal String email, @PathVariable long organizationId, @RequestBody MemberInput input) {
        requireLegacyOrganizations();
        boolean global = platformAccess.isSuperAdmin(email);
        OrganizationView organization = organizationById(organizationId);
        if (!global) requireOrganizationManager(email, organizationId, organization.type());
        String memberEmail = clean(input == null ? null : input.email()).toLowerCase();
        String role = clean(input == null ? null : input.role()).toUpperCase();
        if (!validRole(organization.type(), role) || !validEmail(memberEmail)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail ou papel incompatível com esta organização.");
        Long userId = jdbc.query("SELECT id FROM app_users WHERE LOWER(email)=LOWER(?)", (rs, row) -> rs.getLong(1), memberEmail).stream().findFirst().orElse(null);
        if (userId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A pessoa precisa criar uma conta na Trampolim antes de ser vinculada.");
        jdbc.update("INSERT INTO organization_members(organization_id,user_id,role) VALUES (?,?,?) ON CONFLICT(organization_id,user_id) DO UPDATE SET role=EXCLUDED.role", organizationId, userId, role);
        audit(email, organizationId, "ORGANIZATION_MEMBER_UPSERTED", "USER", userId);
    }

    @PutMapping("/api/legacy/organizations/{organizationId}/companies/{companyId}")
    public void updateCompanyLink(@AuthenticationPrincipal String email, @PathVariable long organizationId, @PathVariable long companyId, @RequestBody StatusInput input) {
        requireLegacyOrganizations();
        boolean global = platformAccess.isSuperAdmin(email);
        if (!global) requireOrganizationManager(email, organizationId, "ASSOCIATION");
        if (input == null || !Set.of("ACTIVE", "PENDING", "ENDED").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de vínculo inválido.");
        if (!global && "ACTIVE".equals(input.status())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A ativação do vínculo precisa ser aprovada pela administração global.");
        int changed = jdbc.update("UPDATE organization_relationships r SET status=? FROM organizations c WHERE r.parent_organization_id=? AND c.legacy_company_id=? AND r.child_organization_id=c.id AND r.relationship_type='ASSOCIATION_MEMBER'", input.status(), organizationId, companyId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado.");
        audit(email, organizationId, "COMPANY_LINK_STATUS_CHANGED", "COMPANY", companyId);
    }

    @GetMapping("/api/legacy/organizations/{organizationId}/members")
    public List<OrganizationMember> organizationMembers(@AuthenticationPrincipal String email, @PathVariable long organizationId) {
        requireLegacyOrganizations();
        boolean global = platformAccess.isSuperAdmin(email);
        OrganizationView org = organizationById(organizationId);
        if (!global) requireOrganizationManager(email, organizationId, org.type());
        return jdbc.query("SELECT u.id,u.full_name,u.email,m.role,m.created_at FROM organization_members m JOIN app_users u ON u.id=m.user_id WHERE m.organization_id=? ORDER BY CASE WHEN m.role LIKE '%ADMIN%' OR m.role='COMPANY_ADMIN' THEN 0 ELSE 1 END,u.full_name", (rs, row) -> new OrganizationMember(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant().toString()), organizationId);
    }

    @DeleteMapping("/api/legacy/organizations/{organizationId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeOrganizationMember(@AuthenticationPrincipal String email, @PathVariable long organizationId, @PathVariable long userId) {
        requireLegacyOrganizations();
        boolean global = platformAccess.isSuperAdmin(email);
        OrganizationView org = organizationById(organizationId);
        if (!global) requireOrganizationManager(email, organizationId, org.type());
        String role = jdbc.query("SELECT role FROM organization_members WHERE organization_id=? AND user_id=?", (rs, row) -> rs.getString(1), organizationId, userId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado."));
        if (role.endsWith("ADMIN") || role.equals("COMPANY_ADMIN")) {
            Integer admins = jdbc.queryForObject("SELECT COUNT(*) FROM organization_members WHERE organization_id=? AND (role LIKE '%ADMIN' OR role='COMPANY_ADMIN')", Integer.class, organizationId);
            if (admins != null && admins <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "A organização precisa manter pelo menos uma pessoa administradora.");
        }
        jdbc.update("DELETE FROM organization_members WHERE organization_id=? AND user_id=?", organizationId, userId);
        audit(email, organizationId, "ORGANIZATION_MEMBER_REMOVED", "USER", userId);
    }

    @GetMapping("/api/legacy/organizations/{organizationId}/companies")
    public List<LinkedCompany> linkedCompanies(@AuthenticationPrincipal String email, @PathVariable long organizationId) {
        requireLegacyOrganizations();
        boolean global = platformAccess.isSuperAdmin(email);
        if (!global) requireOrganizationManager(email, organizationId, "ASSOCIATION");
        return jdbc.query("SELECT c.legacy_company_id,c.name,r.status FROM organization_relationships r JOIN organizations c ON c.id=r.child_organization_id WHERE r.parent_organization_id=? AND r.relationship_type='ASSOCIATION_MEMBER' ORDER BY c.name", (rs, row) -> new LinkedCompany(rs.getLong(1), rs.getString(2), rs.getString(3)), organizationId);
    }

    @GetMapping("/api/legacy/association/organizations/{organizationId}/available-companies")
    public List<CompanyOption> availableCompanies(@AuthenticationPrincipal String email, @PathVariable long organizationId) {
        requireLegacyOrganizations();
        requireOrganizationManager(email, organizationId, "ASSOCIATION");
        return jdbc.query("SELECT c.id,c.trade_name,c.city,c.state,c.industry FROM companies c JOIN organizations o ON o.legacy_company_id=c.id AND o.status='ACTIVE' WHERE NOT EXISTS (SELECT 1 FROM organization_relationships r WHERE r.parent_organization_id=? AND r.child_organization_id=o.id AND r.relationship_type='ASSOCIATION_MEMBER' AND r.status IN ('ACTIVE','PENDING')) ORDER BY c.trade_name", (rs, row) -> new CompanyOption(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)), organizationId);
    }

    @GetMapping("/api/legacy/association/organizations")
    public List<OrganizationView> myAssociations(@AuthenticationPrincipal String email) {
        requireLegacyOrganizations();
        return organizationList("WHERE o.organization_type='ASSOCIATION' AND o.status='ACTIVE' AND EXISTS (SELECT 1 FROM organization_members m JOIN app_users u ON u.id=m.user_id WHERE m.organization_id=o.id AND LOWER(u.email)=LOWER(?) AND m.role IN ('ASSOCIATION_ADMIN','ASSOCIATION_MEMBER'))", new Object[]{email});
    }

    @GetMapping("/api/legacy/association/organizations/{id}/dashboard")
    public AssociationDashboard associationDashboard(@AuthenticationPrincipal String email, @PathVariable long id) {
        requireLegacyOrganizations();
        requireAssociationViewer(email, id);
        List<AssociationDashboard> result = jdbc.query("""
                SELECT o.id,o.name,
                  (SELECT COUNT(*) FROM organization_relationships r JOIN organizations c ON c.id=r.child_organization_id AND c.organization_type='COMPANY' WHERE r.parent_organization_id=o.id AND r.relationship_type='ASSOCIATION_MEMBER' AND r.status='ACTIVE'),
                  (SELECT COUNT(*) FROM organization_relationships r JOIN organizations c ON c.id=r.child_organization_id AND c.organization_type='COMPANY' AND c.status='ACTIVE' WHERE r.parent_organization_id=o.id AND r.relationship_type='ASSOCIATION_MEMBER' AND r.status='ACTIVE'),
                  (SELECT COUNT(*) FROM company_listings l WHERE l.company_id IN (SELECT c.legacy_company_id FROM organization_relationships r JOIN organizations c ON c.id=r.child_organization_id WHERE r.parent_organization_id=o.id AND r.status='ACTIVE') AND l.kind='job' AND l.is_internship=FALSE AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE)),
                  (SELECT COUNT(*) FROM company_listings l WHERE l.company_id IN (SELECT c.legacy_company_id FROM organization_relationships r JOIN organizations c ON c.id=r.child_organization_id WHERE r.parent_organization_id=o.id AND r.status='ACTIVE') AND l.is_internship=TRUE AND l.payment_status='PAID' AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE)),
                  (SELECT COUNT(*) FROM company_applications a JOIN company_listings l ON l.id=a.listing_id WHERE l.company_id IN (SELECT c.legacy_company_id FROM organization_relationships r JOIN organizations c ON c.id=r.child_organization_id WHERE r.parent_organization_id=o.id AND r.status='ACTIVE')),
                  (SELECT COUNT(*) FROM education_contents WHERE status='PUBLISHED')
                FROM organizations o WHERE o.id=? AND o.organization_type='ASSOCIATION' AND o.status='ACTIVE'
                """, (rs, row) -> new AssociationDashboard(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getLong(7), rs.getLong(8)), id);
        if (result.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Associação não encontrada ou inativa.");
        return result.get(0);
    }

    @PostMapping("/api/admin/saas/platform-roles")
    @ResponseStatus(HttpStatus.CREATED)
    public void grantPlatformRole(@AuthenticationPrincipal String email, @RequestBody PlatformRoleInput input) {
        requireSuperAdmin(email);
        if (input == null || !"SUPER_ADMIN".equals(input.role()) || !validEmail(input.email())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Papel global inválido.");
        jdbc.update("INSERT INTO platform_roles(user_id,role,granted_by) SELECT target.id,'SUPER_ADMIN',actor.id FROM app_users target JOIN app_users actor ON LOWER(actor.email)=LOWER(?) WHERE LOWER(target.email)=LOWER(?) ON CONFLICT(user_id,role) DO NOTHING", email, input.email().trim());
        audit(email, null, "PLATFORM_ROLE_GRANTED", "USER", null);
    }

    @GetMapping("/api/admin/saas/platform-roles")
    public List<PlatformAdminView> platformAdmins(@AuthenticationPrincipal String email) {
        requireSuperAdmin(email);
        return jdbc.query("SELECT u.id,u.full_name,u.email,pr.created_at FROM platform_roles pr JOIN app_users u ON u.id=pr.user_id WHERE pr.role='SUPER_ADMIN' ORDER BY u.full_name", (rs, row) -> new PlatformAdminView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant().toString()));
    }

    @DeleteMapping("/api/admin/saas/platform-roles/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePlatformRole(@AuthenticationPrincipal String email, @PathVariable long userId) {
        requireSuperAdmin(email);
        if (jdbc.update("DELETE FROM platform_roles WHERE user_id=? AND role='SUPER_ADMIN'", userId) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        audit(email, null, "PLATFORM_ROLE_REVOKED", "USER", userId);
    }

    private List<OrganizationView> organizationList(String where, Object[] args) {
        String sql = "SELECT o.id,o.name,o.organization_type,o.status,o.legacy_company_id,(SELECT COUNT(*) FROM organization_members m WHERE m.organization_id=o.id),(SELECT COUNT(*) FROM organization_relationships r WHERE r.parent_organization_id=o.id AND r.relationship_type='ASSOCIATION_MEMBER' AND r.status='ACTIVE'),o.created_at FROM organizations o " + where + " ORDER BY o.created_at DESC,o.id";
        return jdbc.query(sql, (rs, row) -> new OrganizationView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), (Long) rs.getObject(5), rs.getLong(6), rs.getLong(7), rs.getTimestamp(8).toInstant().toString()), args);
    }
    private OrganizationView organizationById(long id) {
        return organizationList("WHERE o.id=?", new Object[]{id}).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada."));
    }
    private void requireSuperAdmin(String email) { if (!platformAccess.isSuperAdmin(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito à administração global."); }
    private void requireLegacyOrganizations() { if (!legacyOrganizationsEnabled) throw new ResponseStatusException(HttpStatus.GONE,"A gestão institucional foi transferida para a área Empresas."); }
    private void requireOrganizationManager(String email, long organizationId, String type) {
        if (platformAccess.isSuperAdmin(email)) return;
        Integer allowed = jdbc.queryForObject("SELECT COUNT(*) FROM organizations o JOIN organization_members m ON m.organization_id=o.id JOIN app_users u ON u.id=m.user_id WHERE o.id=? AND o.organization_type=? AND o.status='ACTIVE' AND LOWER(u.email)=LOWER(?) AND ((o.organization_type='ASSOCIATION' AND m.role='ASSOCIATION_ADMIN') OR (o.organization_type='EDUCATIONAL' AND m.role IN ('EDUCATION_ADMIN','PARTNER_MANAGER')) OR (o.organization_type='QUALIFICATION' AND m.role IN ('PARTNER_MANAGER','CONTENT_EDITOR'))) ", Integer.class, organizationId, type, email);
        if (allowed == null || allowed == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não pode administrar esta organização.");
    }
    private void requireAssociationViewer(String email, long organizationId) {
        if (platformAccess.isSuperAdmin(email)) return;
        Integer allowed = jdbc.queryForObject("SELECT COUNT(*) FROM organizations o JOIN organization_members m ON m.organization_id=o.id JOIN app_users u ON u.id=m.user_id WHERE o.id=? AND o.organization_type='ASSOCIATION' AND o.status='ACTIVE' AND LOWER(u.email)=LOWER(?) AND m.role IN ('ASSOCIATION_ADMIN','ASSOCIATION_MEMBER')", Integer.class, organizationId, email);
        if (allowed == null || allowed == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não pode acessar este painel institucional.");
    }
    private void audit(String email, Long organizationId, String eventType, String entityType, Long entityId) {
        jdbc.update("INSERT INTO organization_audit_events(organization_id,actor_user_id,event_type,entity_type,entity_id) SELECT ?,id,?,?,? FROM app_users WHERE LOWER(email)=LOWER(?)", organizationId, eventType, entityType, entityId, email);
    }
    private boolean validRole(String type, String role) { return switch (type) {
        case "ASSOCIATION" -> Set.of("ASSOCIATION_ADMIN", "ASSOCIATION_MEMBER").contains(role);
        case "EDUCATIONAL" -> Set.of("EDUCATION_ADMIN", "CONTENT_EDITOR", "PARTNER_MANAGER").contains(role);
        case "QUALIFICATION" -> Set.of("PARTNER_MANAGER", "CONTENT_EDITOR").contains(role);
        default -> false;
    }; }
    private long count(String sql) { Long result = jdbc.queryForObject(sql, Long.class); return result == null ? 0 : result; }
    private long count(JdbcTemplate source,String sql) { Long result=source.queryForObject(sql,Long.class);return result==null?0:result; }
    private JdbcTemplate required(ObjectProvider<JdbcTemplate> provider,String name) {
        JdbcTemplate target=provider.getIfAvailable();
        if(target==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"O banco de "+name+" não está configurado.");
        return target;
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean validEmail(String email) { return email != null && email.length() <= 180 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); }
}
