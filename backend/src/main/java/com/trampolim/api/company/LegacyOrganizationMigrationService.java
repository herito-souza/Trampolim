package com.trampolim.api.company;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Copies association/partner metadata into Empresas without transferring Oportunidades credentials. */
@Service
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class LegacyOrganizationMigrationService {
    private final JdbcTemplate legacy;
    private final JdbcTemplate empresas;
    private final TransactionTemplate targetTransaction;

    public LegacyOrganizationMigrationService(@Qualifier("jdbcTemplate") JdbcTemplate legacy,
            @Qualifier("empresasJdbcTemplate") JdbcTemplate empresas,
            @Qualifier("empresasTransactionManager") PlatformTransactionManager transactionManager) {
        this.legacy = legacy;
        this.empresas = empresas;
        this.targetTransaction = new TransactionTemplate(transactionManager);
    }

    public record Preview(long organizations, long alreadyImported, long memberships, long relationships,
            long relationshipsWithoutMigratedCompany) {}
    public record MigrationResult(long importedOrganizations, long importedMemberships, long importedRelationships,
            List<String> organizationErrors) {}

    public Preview preview() {
        long organizations = count(legacy, "SELECT COUNT(*) FROM organizations WHERE organization_type <> 'COMPANY'");
        long already = count(empresas, "SELECT COUNT(*) FROM enterprise_organizations WHERE legacy_organization_id IS NOT NULL");
        long memberships = count(legacy, "SELECT COUNT(*) FROM organization_members m JOIN organizations o ON o.id=m.organization_id WHERE o.organization_type <> 'COMPANY'");
        long relationships = count(legacy, "SELECT COUNT(*) FROM organization_relationships r JOIN organizations o ON o.id=r.parent_organization_id WHERE o.organization_type <> 'COMPANY'");
        long missingCompanies = legacy.queryForList("""
                SELECT DISTINCT child.legacy_company_id
                FROM organization_relationships r
                JOIN organizations parent ON parent.id=r.parent_organization_id AND parent.organization_type <> 'COMPANY'
                JOIN organizations child ON child.id=r.child_organization_id AND child.organization_type='COMPANY'
                WHERE child.legacy_company_id IS NOT NULL
                """, Long.class).stream()
                .filter(legacyCompanyId -> !exists(empresas,
                        "SELECT COUNT(*) FROM companies WHERE legacy_company_id=?", legacyCompanyId))
                .count();
        return new Preview(organizations, already, memberships, relationships, missingCompanies);
    }

    /** Safe to repeat. Source records are read-only; target rows use legacy IDs and unique keys. */
    public MigrationResult migrate(String actorEmail) {
        long importedOrganizations = 0;
        long importedMemberships = 0;
        long importedRelationships = 0;
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> organizations = legacy.queryForList("""
                SELECT id,name,organization_type,status,created_at
                FROM organizations WHERE organization_type <> 'COMPANY' ORDER BY id
                """);
        for (Map<String, Object> source : organizations) {
            long legacyId = number(source, "id");
            try {
                long[] counts = targetTransaction.execute(status -> importOrganization(source, actorEmail));
                if (counts != null) {
                    importedOrganizations += counts[0];
                    importedMemberships += counts[1];
                    importedRelationships += counts[2];
                }
            } catch (DataIntegrityViolationException failure) {
                // Keep response free of organization/member data and SQL diagnostics.
                errors.add("Organização legada " + legacyId + ": não importada por conflito ou falha de consistência.");
            }
        }
        return new MigrationResult(importedOrganizations, importedMemberships, importedRelationships, List.copyOf(errors));
    }

    private long[] importOrganization(Map<String, Object> source, String actorEmail) {
        long legacyOrganizationId = number(source, "id");
        List<Long> targetIds = empresas.query("SELECT id FROM enterprise_organizations WHERE legacy_organization_id=?",
                (rs, row) -> rs.getLong(1), legacyOrganizationId);
        long targetId;
        long organizationCount = 0;
        if (targetIds.isEmpty()) {
            targetId = empresas.queryForObject("""
                    INSERT INTO enterprise_organizations(legacy_organization_id,name,organization_type,status,created_at)
                    VALUES (?,?,?,?,?) RETURNING id
                    """, Long.class, legacyOrganizationId, text(source, "name"), text(source, "organization_type"),
                    text(source, "status"), timestamp(source.get("created_at")));
            organizationCount = 1;
            empresas.update("INSERT INTO enterprise_organization_audit_events(organization_id,actor_email,event_type,entity_type,entity_reference) VALUES (?,?, 'LEGACY_ORGANIZATION_IMPORTED','ORGANIZATION',?)",
                    targetId, actorEmail, Long.toString(legacyOrganizationId));
        } else {
            targetId = targetIds.get(0);
        }

        int memberCount = 0;
        for (Map<String, Object> member : legacy.queryForList("""
                SELECT u.id,u.email,u.full_name,m.role,m.created_at
                FROM organization_members m JOIN app_users u ON u.id=m.user_id
                WHERE m.organization_id=? ORDER BY u.id
                """, legacyOrganizationId)) {
            memberCount += empresas.update("""
                    INSERT INTO enterprise_organization_members(organization_id,legacy_user_id,email,full_name,role,created_at)
                    VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING
                    """, targetId, number(member, "id"), text(member, "email").toLowerCase(Locale.ROOT), text(member, "full_name"),
                    text(member, "role"), timestamp(member.get("created_at")));
        }

        int relationshipCount = 0;
        for (Map<String, Object> relationship : legacy.queryForList("""
                SELECT child.id AS child_id,child.legacy_company_id,r.relationship_type,r.status,r.created_at
                FROM organization_relationships r
                JOIN organizations child ON child.id=r.child_organization_id AND child.organization_type='COMPANY'
                WHERE r.parent_organization_id=? AND child.legacy_company_id IS NOT NULL
                """, legacyOrganizationId)) {
            relationshipCount += empresas.update("""
                    INSERT INTO enterprise_organization_companies(organization_id,company_id,legacy_child_organization_id,relationship_type,status,created_at)
                    SELECT ?,id,?,?,?,? FROM companies WHERE legacy_company_id=?
                    ON CONFLICT(organization_id,company_id,relationship_type) DO NOTHING
                    """, targetId, number(relationship, "child_id"), text(relationship, "relationship_type"),
                    text(relationship, "status"), timestamp(relationship.get("created_at")),
                    number(relationship, "legacy_company_id"));
        }
        return new long[]{organizationCount, memberCount, relationshipCount};
    }

    private long count(JdbcTemplate jdbc, String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        return result == null ? 0 : result;
    }
    private boolean exists(JdbcTemplate jdbc, String sql, Object argument) {
        Long result = jdbc.queryForObject(sql, Long.class, argument);
        return result != null && result > 0;
    }
    private long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }
    private String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }
    private Timestamp timestamp(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp;
        if (value instanceof OffsetDateTime offset) return Timestamp.from(offset.toInstant());
        if (value instanceof Instant instant) return Timestamp.from(instant);
        if (value instanceof LocalDateTime local) return Timestamp.valueOf(local);
        return Timestamp.from(Instant.now());
    }
}
