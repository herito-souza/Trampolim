package com.trampolim.api.company;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Copies legacy company data without copying passwords, JWTs, sessions or usable payment confirmation. */
@Service
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class LegacyCompanyMigrationService {
    private final JdbcTemplate legacy;
    private final JdbcTemplate empresas;
    private final PasswordEncoder passwords;
    private final TransactionTemplate targetTransaction;

    public LegacyCompanyMigrationService(@Qualifier("jdbcTemplate") JdbcTemplate legacy,
            @Qualifier("empresasJdbcTemplate") JdbcTemplate empresas,
            @Qualifier("empresasTransactionManager") PlatformTransactionManager transactionManager,
            PasswordEncoder passwords) {
        this.legacy = legacy;
        this.empresas = empresas;
        this.passwords = passwords;
        this.targetTransaction = new TransactionTemplate(transactionManager);
    }

    public record Preview(long sourceCompanies, long alreadyImported, long importable, long unmatchedOwners,
            long invalidCnpj, long targetCnpjConflicts, long targetOwnerConflicts) {}
    public record MigrationResult(long imported, long alreadyImported, List<String> companyErrors) {}

    public Preview preview() {
        long sourceCount = count(legacy, "SELECT COUNT(*) FROM companies");
        long imported = count(empresas, "SELECT COUNT(*) FROM companies WHERE legacy_company_id IS NOT NULL");
        long unmatched = 0, invalidCnpj = 0, conflicts = 0, ownerConflicts = 0, importable = 0;
        for (Map<String, Object> row : legacy.queryForList("SELECT c.id,c.cnpj,u.email,u.account_type FROM companies c LEFT JOIN app_users u ON u.id=c.user_id")) {
            long id = number(row, "id", 0L);
            if (exists(empresas, "SELECT COUNT(*) FROM companies WHERE legacy_company_id=?", id)) continue;
            if (string(row, "email").isBlank() || !"COMPANY".equalsIgnoreCase(string(row, "account_type"))) { unmatched++; continue; }
            String cnpj = string(row, "cnpj").replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (!cnpj.matches("[0-9]{14}")) { invalidCnpj++; continue; }
            if (exists(empresas, "SELECT COUNT(*) FROM companies WHERE cnpj=? AND legacy_company_id IS NULL", cnpj)) { conflicts++; continue; }
            if (exists(empresas, "SELECT COUNT(*) FROM companies c JOIN company_users u ON u.id=c.owner_user_id WHERE LOWER(u.email)=LOWER(?) AND c.cnpj<>?", string(row, "email").trim(), cnpj)) { ownerConflicts++; continue; }
            importable++;
        }
        return new Preview(sourceCount, imported, importable, unmatched, invalidCnpj, conflicts, ownerConflicts);
    }

    /** Safe to retry: each imported source ID is unique in the target and each company is committed separately. */
    public MigrationResult migrate(String migrationActor) {
        List<Map<String, Object>> companies = legacy.queryForList("""
                SELECT c.*, u.email AS legacy_owner_email, u.full_name AS legacy_owner_name
                FROM companies c JOIN app_users u ON u.id=c.user_id AND u.account_type='COMPANY'
                ORDER BY c.id
                """);
        Boolean legacyPaymentTableExists = legacy.queryForObject("SELECT to_regclass('public.company_listing_payments') IS NOT NULL", Boolean.class);
        boolean importPaymentHistory = Boolean.TRUE.equals(legacyPaymentTableExists);
        long imported = 0;
        long alreadyImported = 0;
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> company : companies) {
            long legacyId = number(company, "id", 0L);
            String candidateCnpj = string(company, "cnpj").replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            // Preview classifies invalid rows; execution leaves them in the source for reconciliation.
            if (string(company, "legacy_owner_email").isBlank() || !candidateCnpj.matches("[0-9]{14}")) continue;
            try {
                Boolean wasImported = targetTransaction.execute(status -> importCompany(company, migrationActor, importPaymentHistory));
                if (Boolean.TRUE.equals(wasImported)) imported++;
                else alreadyImported++;
            } catch (DataIntegrityViolationException | IllegalStateException failure) {
                // Avoid returning personal data, CNPJ, SQL details or credentials to the caller.
                errors.add("Empresa legada " + legacyId + ": não importada por conflito ou falha de consistência; corrija a origem e tente novamente.");
            }
        }
        return new MigrationResult(imported, alreadyImported, List.copyOf(errors));
    }

    private boolean importCompany(Map<String, Object> source, String migrationActor, boolean importPaymentHistory) {
        long sourceCompanyId = number(source, "id", 0L);
        if (exists(empresas, "SELECT COUNT(*) FROM companies WHERE legacy_company_id=?", sourceCompanyId)) return false;

        String email = string(source, "legacy_owner_email").trim().toLowerCase(Locale.ROOT);
        String cnpj = string(source, "cnpj").replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (email.isBlank() || !cnpj.matches("[0-9]{14}")) throw new IllegalStateException("Legacy company owner or CNPJ is not importable.");

        List<Long> targetUser = empresas.query("SELECT id FROM company_users WHERE LOWER(email)=LOWER(?)", (rs, row) -> rs.getLong(1), email);
        long userId;
        if (targetUser.isEmpty()) {
            // BCrypt truncates at 72 bytes; a single UUID remains random and cannot be logged in
            // through the imported account (which stays pending and email-unverified).
            String unreachablePassword = passwords.encode(UUID.randomUUID().toString());
            userId = empresas.queryForObject("""
                    INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at)
                    VALUES (?,?,?,FALSE,'PENDING',NULL) RETURNING id
                    """, Long.class, email, unreachablePassword, truncate(string(source, "legacy_owner_name"), 120));
        } else {
            userId = targetUser.get(0);
        }

        Long companyId = empresas.queryForObject("""
                INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,website,industry,address,city,state,
                                     verification_status,profile_description,legacy_company_id,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,'PENDING',?,?,?) RETURNING id
                """, Long.class, userId,
                truncate(string(source, "legal_name"), 180), truncate(string(source, "trade_name"), 180), cnpj,
                truncate(string(source, "phone"), 40), truncateNullable(string(source, "website"), 255),
                truncate(string(source, "industry"), 100), truncate(string(source, "address"), 255),
                truncate(string(source, "city"), 100), truncate(string(source, "state"), 2).toUpperCase(Locale.ROOT),
                truncateNullable(string(source, "profile_description"), 2000), sourceCompanyId, timestamp(source, "created_at"));
        empresas.update("INSERT INTO company_members(company_id,user_id,role) VALUES (?,?,'OWNER') ON CONFLICT DO NOTHING", companyId, userId);
        importLegacyMembers(sourceCompanyId, companyId);
        importListings(sourceCompanyId, companyId, importPaymentHistory);
        importInvites(sourceCompanyId, companyId);
        empresas.update("INSERT INTO company_audit_events(company_id,event_type,details) VALUES (?, 'LEGACY_COMPANY_IMPORTED', jsonb_build_object('legacyCompanyId', ?, 'actorEmail', ?))",
                companyId, sourceCompanyId, migrationActor);
        return true;
    }

    private void importListings(long sourceCompanyId, long companyId, boolean importPaymentHistory) {
        List<Map<String, Object>> listings = legacy.queryForList("SELECT * FROM company_listings WHERE company_id=? ORDER BY id", sourceCompanyId);
        for (Map<String, Object> listing : listings) {
            long sourceListingId = number(listing, "id", 0L);
            List<Long> existing = empresas.query("SELECT id FROM company_listings WHERE legacy_listing_id=?", (rs, row) -> rs.getLong(1), sourceListingId);
            if (!existing.isEmpty()) continue;
            boolean internship = bool(listing, "is_internship", false);
            Long targetListingId = empresas.queryForObject("""
                    INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,
                        contact_email,salary,contract_type,work_schedule,workload,prerequisites,benefits,course_price,
                        certificate,expires_at,price_cents,payment_status,publication_status,is_internship,accepted_courses,
                        headcount,application_deadline,created_at,updated_at,legacy_listing_id,
                        legacy_publication_status,legacy_payment_status,legacy_price_cents)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'NOT_PROCESSED','DRAFT',?,?,?,?,?,?,?,?,?,?) RETURNING id
                    """, Long.class, companyId, string(listing, "kind"), truncate(string(listing, "title"), 180),
                    truncate(string(listing, "area"), 80), truncate(string(listing, "description"), 5000),
                    truncate(string(listing, "requirements"), 3000), truncate(string(listing, "location"), 180),
                    truncate(string(listing, "modality"), 40), truncate(string(listing, "url"), 1000),
                    truncate(string(listing, "contact_email"), 180), truncateNullable(string(listing, "salary"), 100),
                    truncateNullable(string(listing, "contract_type"), 80), truncateNullable(string(listing, "work_schedule"), 100),
                    truncateNullable(string(listing, "workload"), 100), truncateNullable(string(listing, "prerequisites"), 2000),
                    truncateNullable(string(listing, "benefits"), 2000), truncateNullable(string(listing, "course_price"), 100),
                    truncateNullable(string(listing, "certificate"), 200), listing.get("expires_at"),
                    internship ? 5000 : number(listing, "price_cents", 10000), internship,
                    truncateNullable(string(listing, "accepted_courses"), 1000), nullableNumber(listing, "headcount"),
                    listing.get("application_deadline"), timestamp(listing, "created_at"), timestamp(listing, "updated_at"),
                    sourceListingId, truncateNullable(string(listing, "publication_status"), 32),
                    truncateNullable(string(listing, "payment_status"), 32), number(listing, "price_cents", 10000));
            importListingEvents(sourceListingId, targetListingId);
            importApplications(sourceListingId, targetListingId);
            if (importPaymentHistory) importPayments(sourceListingId, companyId, targetListingId);
        }
    }

    private void importListingEvents(long sourceListingId, long targetListingId) {
        for (Map<String, Object> event : legacy.queryForList("SELECT * FROM company_listing_events WHERE listing_id=? ORDER BY id", sourceListingId)) {
            String eventType = "VIEW".equals(string(event, "event_type")) ? "LISTING_VIEW" : string(event, "event_type");
            empresas.update("INSERT INTO company_listing_events(listing_id,event_type,actor_email,details,created_at,legacy_event_id) VALUES (?,?,?,?,?,?) ON CONFLICT(legacy_event_id) DO NOTHING",
                    targetListingId, eventType, truncateNullable(string(event, "actor_email"), 180),
                    truncateNullable(string(event, "details"), 1000), timestamp(event, "created_at"), number(event, "id", 0L));
        }
    }

    private void importApplications(long sourceListingId, long targetListingId) {
        for (Map<String, Object> application : legacy.queryForList("SELECT * FROM company_applications WHERE listing_id=? ORDER BY id", sourceListingId)) {
            empresas.update("""
                    INSERT INTO company_applications(listing_id,candidate_name,candidate_email,candidate_phone,message,
                        status,consent_at,created_at,updated_at,legacy_application_id)
                    VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT(legacy_application_id) DO NOTHING
                    """, targetListingId, truncate(string(application, "candidate_name"), 120),
                    truncate(string(application, "candidate_email"), 180), truncateNullable(string(application, "candidate_phone"), 40),
                    truncateNullable(string(application, "message"), 2000), string(application, "status"),
                    timestamp(application, "consent_at"), timestamp(application, "created_at"),
                    timestamp(application, "updated_at"), number(application, "id", 0L));
        }
    }

    private void importPayments(long sourceListingId, long companyId, long targetListingId) {
        for (Map<String, Object> payment : legacy.queryForList("SELECT * FROM company_listing_payments WHERE listing_id=? ORDER BY id", sourceListingId)) {
            empresas.update("""
                    INSERT INTO company_legacy_payment_history(company_id,listing_id,legacy_payment_id,provider,
                        external_reference,provider_order_id,amount_cents,legacy_status,provider_status,created_at,updated_at,paid_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(legacy_payment_id) DO NOTHING
                    """, companyId, targetListingId, number(payment, "id", 0L),
                    truncateNullable(string(payment, "provider"), 40), truncateNullable(string(payment, "external_reference"), 120),
                    truncateNullable(string(payment, "provider_order_id"), 120), nullableNumber(payment, "amount_cents"),
                    truncateNullable(string(payment, "status"), 40), truncateNullable(string(payment, "provider_status"), 80),
                    payment.get("created_at"), payment.get("updated_at"), payment.get("paid_at"));
        }
    }

    private void importLegacyMembers(long sourceCompanyId, long companyId) {
        for (Map<String, Object> member : legacy.queryForList("""
                SELECT u.id AS user_id,u.email,u.full_name,m.role
                FROM company_members m JOIN app_users u ON u.id=m.user_id WHERE m.company_id=?
                """, sourceCompanyId)) {
            empresas.update("INSERT INTO company_legacy_members(company_id,legacy_user_id,email,full_name,legacy_role) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                    companyId, number(member, "user_id", 0L), truncate(string(member, "email"), 180),
                    truncateNullable(string(member, "full_name"), 120), truncate(string(member, "role"), 32));
        }
    }

    private void importInvites(long sourceCompanyId, long companyId) {
        for (Map<String, Object> invite : legacy.queryForList("SELECT * FROM company_invites WHERE company_id=? ORDER BY id", sourceCompanyId)) {
            empresas.update("INSERT INTO company_legacy_invites(company_id,legacy_invite_id,email,legacy_role,expires_at,accepted_at) VALUES (?,?,?,?,?,?) ON CONFLICT(legacy_invite_id) DO NOTHING",
                    companyId, number(invite, "id", 0L), truncate(string(invite, "email"), 180),
                    truncate(string(invite, "role"), 32), invite.get("expires_at"), invite.get("accepted_at"));
        }
    }

    private long count(JdbcTemplate jdbc, String sql) { Long value = jdbc.queryForObject(sql, Long.class); return value == null ? 0 : value; }
    private boolean exists(JdbcTemplate jdbc, String sql, Object argument) { return count(jdbc, sql, argument) > 0; }
    private long count(JdbcTemplate jdbc, String sql, Object argument) { Long value = jdbc.queryForObject(sql, Long.class, argument); return value == null ? 0 : value; }
    private boolean exists(JdbcTemplate jdbc, String sql, Object first, Object second) { return count(jdbc, sql, first, second) > 0; }
    private long count(JdbcTemplate jdbc, String sql, Object first, Object second) { Long value = jdbc.queryForObject(sql, Long.class, first, second); return value == null ? 0 : value; }
    private String string(Map<String, Object> row, String key) { Object value = row.get(key); return value == null ? "" : value.toString(); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private String truncateNullable(String value, int max) { return value.isBlank() ? null : truncate(value, max); }
    private long number(Map<String, Object> row, String key, long fallback) { Object value = row.get(key); return value instanceof Number number ? number.longValue() : fallback; }
    private Integer nullableNumber(Map<String, Object> row, String key) { Object value = row.get(key); return value instanceof Number number ? number.intValue() : null; }
    private boolean bool(Map<String, Object> row, String key, boolean fallback) { Object value = row.get(key); return value instanceof Boolean bool ? bool : fallback; }
    private Timestamp timestamp(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Timestamp timestamp) return timestamp;
        if (value instanceof OffsetDateTime offset) return Timestamp.from(offset.toInstant());
        if (value instanceof Instant instant) return Timestamp.from(instant);
        if (value instanceof LocalDateTime local) return Timestamp.valueOf(local);
        return Timestamp.from(Instant.now());
    }
}
