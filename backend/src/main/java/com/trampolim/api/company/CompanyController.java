package com.trampolim.api.company;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/company")
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class CompanyController {
    private final JdbcTemplate jdbc;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;

    public CompanyController(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record CompanySummary(String legalName, String tradeName, String cnpj, String phone, String industry, String city, String state, String website, String description, String publicSlug, String role) {}
    public record CompanyProfileInput(String tradeName, String industry, String phone, String website, String city, String state, String description) {}
    public record ListingInput(String kind, String title, String area, String description, String requirements,
            String location, String modality, String url, String contactEmail, String salary, String contractType,
            String workSchedule, String workload, String prerequisites, String benefits, String coursePrice, String certificate,
            boolean isInternship, String acceptedCourses, Integer headcount, LocalDate applicationDeadline) {}
    public record StatusInput(String status) {}
    public record RenewInput(LocalDate expiresAt) {}
    public record MemberInviteInput(String email, String role) {}
    public record ListingView(long id, String kind, String title, String area, String description, String requirements,
            String location, String modality, String url, String contactEmail, String salary, String contractType,
            String workSchedule, String workload, String prerequisites, String benefits, String coursePrice,
            String certificate, String expiresAt, int priceCents, String paymentStatus, String publicationStatus,
            String createdAt, String updatedAt, long views, long applicationClicks, long applications,
            boolean isInternship, String acceptedCourses, Integer headcount, String applicationDeadline, String checkoutUrl) {}
    public record MemberView(String name, String email, String role, String createdAt) {}
    public record ApplicationView(long id, long listingId, String listingTitle, String kind, String name, String email, String phone, String message, String status, String createdAt) {}
    public record InviteView(String email, String role, String expiresAt, String inviteUrl) {}
    public record CommercialInterestInput(String offerType, String title, String details) {}
    public record CommercialInterestView(long id, String offerType, String title, String details, String createdAt) {}
    public record AnalyticsDay(String date, long views, long clicks, long applications) {}
    public record ListingAnalytics(long listingId, String title, String kind, long views, long clicks, long applications) {}
    public record CompanyAnalytics(int days, List<AnalyticsDay> daily, List<ListingAnalytics> listings) {}
    private record Membership(long companyId, String role) {}

    @GetMapping("/dashboard")
    public CompanySummary company(@AuthenticationPrincipal String email) {
        Membership member = requireCompany(email);
        // Recupera contas empresariais antigas cujo slug não foi preenchido.
        jdbc.update("UPDATE companies SET public_slug=gen_random_uuid()::text WHERE id=? AND COALESCE(public_slug, '')=''", member.companyId());
        return jdbc.queryForObject("SELECT c.legal_name, c.trade_name, c.cnpj, c.phone, c.industry, c.city, c.state, c.website, c.profile_description, c.public_slug FROM companies c WHERE c.id=?",
                (rs, row) -> new CompanySummary(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), member.role().toUpperCase()), member.companyId());
    }

    @PutMapping("/profile")
    @Transactional(transactionManager = "empresasTransactionManager")
    public CompanySummary updateCompany(@AuthenticationPrincipal String email, @RequestBody CompanyProfileInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER");
        if (input == null || blank(input.tradeName()) || blank(input.industry()) || blank(input.phone()) || blank(input.city()) || input.state() == null || !input.state().matches("(?i)[a-z]{2}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome público, setor, telefone, cidade e UF.");
        validateOptionalUrl(input.website());
        jdbc.update("UPDATE companies SET trade_name=?, industry=?, phone=?, website=?, city=?, state=?, profile_description=? WHERE id=?",
                input.tradeName().trim(), input.industry().trim(), input.phone().trim(), clean(input.website()), input.city().trim(), input.state().toUpperCase(), limit(input.description(), 2000), member.companyId());
        return company(email);
    }

    @GetMapping("/listings")
    public List<ListingView> listings(@AuthenticationPrincipal String email) {
        Membership member = requireCompany(email);
        return jdbc.query(listingSql() + " WHERE l.company_id=? ORDER BY l.created_at DESC", listingMapper(), member.companyId());
    }

    @PostMapping("/listings")
    @Transactional(transactionManager = "empresasTransactionManager")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingView createListing(@AuthenticationPrincipal String email, @RequestBody ListingInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER"); validate(input);
        LocalDate deadline = input.applicationDeadline();
        LocalDate expiresAt = input.isInternship() ? (deadline == null ? today().plusDays(30) : deadline) : null;
        Long id = jdbc.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,salary,contract_type,work_schedule,workload,prerequisites,benefits,course_price,certificate,payment_status,publication_status,is_internship,accepted_courses,headcount,application_deadline,expires_at,price_cents) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id",
                Long.class, member.companyId(), input.kind(), input.title().trim(), input.area().trim(), input.description().trim(), input.requirements().trim(), input.location().trim(), input.modality(), clean(input.url()), input.contactEmail().trim().toLowerCase(), clean(input.salary()), input.isInternship() ? "Estágio" : clean(input.contractType()), clean(input.workSchedule()), clean(input.workload()), clean(input.prerequisites()), clean(input.benefits()), clean(input.coursePrice()), clean(input.certificate()), input.isInternship() ? "PENDING" : "NOT_PROCESSED", input.isInternship() ? "AWAITING_PAYMENT" : "DRAFT", input.isInternship(), clean(input.acceptedCourses()), input.headcount(), deadline, expiresAt, input.isInternship() ? 5000 : 10000);
        return listing(member.companyId(), id);
    }

    @PutMapping("/listings/{id}")
    @Transactional(transactionManager = "empresasTransactionManager")
    public ListingView updateListing(@AuthenticationPrincipal String email, @PathVariable long id, @RequestBody ListingInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER"); validate(input); ownListing(member.companyId(), id);
        boolean internship = jdbc.queryForObject("SELECT is_internship FROM company_listings WHERE id=? AND company_id=?", Boolean.class, id, member.companyId());
        if (internship && "PAID".equals(jdbc.queryForObject("SELECT payment_status FROM company_listings WHERE id=?", String.class, id)) && "PUBLISHED".equals(jdbc.queryForObject("SELECT publication_status FROM company_listings WHERE id=?", String.class, id))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Para alterar uma vaga de estágio já publicada, encerre-a e cadastre uma nova divulgação.");
        jdbc.update("UPDATE company_listings SET kind=?, title=?, area=?, description=?, requirements=?, location=?, modality=?, url=?, contact_email=?, salary=?, contract_type=?, work_schedule=?, workload=?, prerequisites=?, benefits=?, course_price=?, certificate=?,accepted_courses=?,headcount=?,application_deadline=?,expires_at=CASE WHEN is_internship THEN COALESCE(?,expires_at) ELSE expires_at END,updated_at=CURRENT_TIMESTAMP WHERE id=? AND company_id=?",
                input.kind(), input.title().trim(), input.area().trim(), input.description().trim(), input.requirements().trim(), input.location().trim(), input.modality(), clean(input.url()), input.contactEmail().trim().toLowerCase(), clean(input.salary()), internship ? "Estágio" : clean(input.contractType()), clean(input.workSchedule()), clean(input.workload()), clean(input.prerequisites()), clean(input.benefits()), clean(input.coursePrice()), clean(input.certificate()), clean(input.acceptedCourses()), input.headcount(), input.applicationDeadline(), input.applicationDeadline(), id, member.companyId());
        return listing(member.companyId(), id);
    }

    @PutMapping("/listings/{id}/status")
    @Transactional(transactionManager = "empresasTransactionManager")
    public ListingView updateStatus(@AuthenticationPrincipal String email, @PathVariable long id, @RequestBody StatusInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER"); ownListing(member.companyId(), id);
        if (input == null || !Set.of("DRAFT", "PUBLISHED", "PAUSED", "CLOSED").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um status de publicação válido.");
        if ("PUBLISHED".equals(input.status())) {
            if (!companyApproved(member.companyId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A empresa precisa ser aprovada antes de publicar oportunidades.");
            List<Object[]> details = jdbc.query("SELECT expires_at,is_internship,payment_status FROM company_listings WHERE id=?", (rs, row) -> new Object[]{rs.getDate(1) == null ? null : rs.getDate(1).toLocalDate(), rs.getBoolean(2), rs.getString(3)}, id);
            LocalDate expiry = (LocalDate) details.get(0)[0];
            if ((boolean) details.get(0)[1] && !"PAID".equals(details.get(0)[2])) throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirme o pagamento de R$ 50,00 antes de publicar a vaga de estágio.");
            if (expiry != null && expiry.isBefore(today())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renove o prazo do anúncio antes de publicá-lo.");
        }
        jdbc.update("UPDATE company_listings SET publication_status=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND company_id=?", input.status(), id, member.companyId());
        return listing(member.companyId(), id);
    }

    @PutMapping("/listings/{id}/renew")
    @Transactional(transactionManager = "empresasTransactionManager")
    public ListingView renew(@AuthenticationPrincipal String email, @PathVariable long id, @RequestBody RenewInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER"); ownListing(member.companyId(), id);
        if (jdbc.queryForObject("SELECT is_internship FROM company_listings WHERE id=?", Boolean.class, id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Para divulgar uma nova vaga de estágio, cadastre uma nova oportunidade e confirme a taxa de R$ 50,00.");
        if (input == null || input.expiresAt() == null || input.expiresAt().isBefore(today().plusDays(1))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma nova data de encerramento no futuro.");
        jdbc.update("UPDATE company_listings SET expires_at=?, publication_status='PUBLISHED', updated_at=CURRENT_TIMESTAMP WHERE id=? AND company_id=?", input.expiresAt(), id, member.companyId());
        return listing(member.companyId(), id);
    }

    @GetMapping("/applications")
    public List<ApplicationView> applications(@AuthenticationPrincipal String email) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER");
        return jdbc.query("SELECT a.id, a.listing_id, l.title, l.kind, a.candidate_name, a.candidate_email, a.candidate_phone, a.message, a.status, a.created_at FROM company_applications a JOIN company_listings l ON l.id=a.listing_id WHERE l.company_id=? ORDER BY a.created_at DESC",
                (rs, row) -> new ApplicationView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7) == null ? "" : rs.getString(7), rs.getString(8) == null ? "" : rs.getString(8), rs.getString(9), rs.getTimestamp(10).toInstant().toString()), member.companyId());
    }

    @PutMapping("/applications/{id}/status")
    @Transactional(transactionManager = "empresasTransactionManager")
    public java.util.Map<String, String> applicationStatus(@AuthenticationPrincipal String email, @PathVariable long id, @RequestBody StatusInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER");
        if (input == null || !Set.of("RECEIVED", "IN_REVIEW", "CONTACTED", "ACCEPTED", "DECLINED").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma etapa válida para a candidatura.");
        int updated = jdbc.update("UPDATE company_applications a SET status=?, updated_at=CURRENT_TIMESTAMP FROM company_listings l WHERE a.listing_id=l.id AND a.id=? AND l.company_id=?", input.status(), id, member.companyId());
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidatura não encontrada.");
        return java.util.Map.of("status", input.status());
    }

    @GetMapping("/commercial-interests")
    public List<CommercialInterestView> commercialInterests(@AuthenticationPrincipal String email) {
        Membership member = requireCompany(email);
        return jdbc.query("SELECT id, offer_type, title, details, created_at FROM company_commercial_interests WHERE company_id=? ORDER BY created_at DESC",
                (rs, row) -> new CommercialInterestView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant().toString()), member.companyId());
    }

    @PostMapping("/commercial-interests")
    @Transactional(transactionManager = "empresasTransactionManager")
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialInterestView registerCommercialInterest(@AuthenticationPrincipal String email, @RequestBody CommercialInterestInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER");
        Set<String> offerTypes = Set.of("PLAN", "HIGHLIGHT", "RECRUITMENT", "COURSE_PARTNERSHIP", "INSTITUTIONAL", "ANALYTICS");
        if (input == null || !offerTypes.contains(clean(input.offerType())) || blank(input.title()) || input.title().length() > 160 || blank(input.details()) || input.details().length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a opção comercial e os detalhes (até 2.000 caracteres).");
        CommercialInterestView saved = jdbc.queryForObject("INSERT INTO company_commercial_interests(company_id, offer_type, title, details) VALUES (?, ?, ?, ?) RETURNING id, offer_type, title, details, created_at",
                (rs, row) -> new CommercialInterestView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant().toString()),
                member.companyId(), input.offerType(), input.title().trim(), input.details().trim());
        return saved;
    }

    @DeleteMapping("/commercial-interests/{id}")
    @Transactional(transactionManager = "empresasTransactionManager")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommercialInterest(@AuthenticationPrincipal String email, @PathVariable long id) {
        Membership member = requireCompany(email); requireRole(member, "OWNER", "RECRUITER");
        int deleted = jdbc.update("DELETE FROM company_commercial_interests WHERE id=? AND company_id=?", id, member.companyId());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Interesse comercial não encontrado.");
    }

    @GetMapping("/analytics")
    public CompanyAnalytics analytics(@AuthenticationPrincipal String email, @RequestParam(defaultValue = "30") int days) {
        Membership member = requireCompany(email);
        if (!Set.of(7, 30, 90).contains(days)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um período de 7, 30 ou 90 dias.");
        Instant since = Instant.now().minusSeconds(days * 24L * 60 * 60);
        List<AnalyticsDay> daily = jdbc.query("""
                SELECT d.day::date,
                       COALESCE(v.total, 0), COALESCE(c.total, 0), COALESCE(a.total, 0)
                FROM generate_series((CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date - (? - 1), (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date, INTERVAL '1 day') AS d(day)
                LEFT JOIN (SELECT (e.created_at AT TIME ZONE 'America/Sao_Paulo')::date AS metric_day, COUNT(*) AS total
                           FROM company_listing_events e JOIN company_listings l ON l.id=e.listing_id
                           WHERE l.company_id=? AND e.event_type='LISTING_VIEW' AND e.created_at>=? GROUP BY 1) v ON v.metric_day=d.day::date
                LEFT JOIN (SELECT (e.created_at AT TIME ZONE 'America/Sao_Paulo')::date AS metric_day, COUNT(*) AS total
                           FROM company_listing_events e JOIN company_listings l ON l.id=e.listing_id
                           WHERE l.company_id=? AND e.event_type='APPLICATION_CLICK' AND e.created_at>=? GROUP BY 1) c ON c.metric_day=d.day::date
                LEFT JOIN (SELECT (a.created_at AT TIME ZONE 'America/Sao_Paulo')::date AS metric_day, COUNT(*) AS total
                           FROM company_applications a JOIN company_listings l ON l.id=a.listing_id
                           WHERE l.company_id=? AND a.created_at>=? GROUP BY 1) a ON a.metric_day=d.day::date
                ORDER BY d.day
                """, (rs, row) -> new AnalyticsDay(rs.getDate(1).toLocalDate().toString(), rs.getLong(2), rs.getLong(3), rs.getLong(4)),
                days, member.companyId(), java.sql.Timestamp.from(since), member.companyId(), java.sql.Timestamp.from(since), member.companyId(), java.sql.Timestamp.from(since));
        List<ListingAnalytics> listingStats = jdbc.query("""
                SELECT l.id, l.title, l.kind,
                       (SELECT COUNT(*) FROM company_listing_events e WHERE e.listing_id=l.id AND e.event_type='LISTING_VIEW' AND e.created_at>=?),
                       (SELECT COUNT(*) FROM company_listing_events e WHERE e.listing_id=l.id AND e.event_type='APPLICATION_CLICK' AND e.created_at>=?),
                       (SELECT COUNT(*) FROM company_applications a WHERE a.listing_id=l.id AND a.created_at>=?)
                FROM company_listings l WHERE l.company_id=? ORDER BY l.created_at DESC
                """, (rs, row) -> new ListingAnalytics(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5), rs.getLong(6)),
                java.sql.Timestamp.from(since), java.sql.Timestamp.from(since), java.sql.Timestamp.from(since), member.companyId());
        return new CompanyAnalytics(days, daily, listingStats);
    }

    @GetMapping("/members")
    public List<MemberView> members(@AuthenticationPrincipal String email) {
        Membership member = requireCompany(email);
        return jdbc.query("SELECT u.full_name, u.email, m.role, m.created_at FROM company_members m JOIN company_users u ON u.id=m.user_id WHERE m.company_id=? ORDER BY CASE m.role WHEN 'OWNER' THEN 0 ELSE 1 END, u.full_name",
                (rs, row) -> new MemberView(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant().toString()), member.companyId());
    }

    @PostMapping("/invites")
    @Transactional(transactionManager = "empresasTransactionManager")
    public InviteView invite(@AuthenticationPrincipal String email, @RequestBody MemberInviteInput input) {
        Membership member = requireCompany(email); requireRole(member, "OWNER");
        String inviteEmail = input == null ? "" : clean(input.email()).toLowerCase(); String role = input == null ? "" : clean(input.role()).toUpperCase();
        if (!inviteEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido para o convite.");
        if (!Set.of("RECRUITER", "ANALYST").contains(role)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha o perfil recrutador ou analista.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM company_members m JOIN company_users u ON u.id=m.user_id WHERE m.company_id=? AND u.email=?", Integer.class, member.companyId(), inviteEmail) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta pessoa já participa da equipe.");
        String token = UUID.randomUUID().toString(); Instant expires = Instant.now().plusSeconds(7 * 24 * 60 * 60);
        jdbc.update("INSERT INTO company_invites(company_id, email, role, token_hash, expires_at) VALUES (?, ?, ?, ?, ?)", member.companyId(), inviteEmail, role, sha256(token), expires);
        return new InviteView(inviteEmail, role, expires.toString(), frontendUrl + "/?companyInvite=" + token);
    }

    @GetMapping("/invites")
    public List<InviteView> invites(@AuthenticationPrincipal String email) {
        Membership member = requireCompany(email); requireRole(member, "OWNER");
        return jdbc.query("SELECT email, role, expires_at FROM company_invites WHERE company_id=? AND accepted_at IS NULL AND expires_at>CURRENT_TIMESTAMP ORDER BY created_at DESC",
                (rs, row) -> new InviteView(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant().toString(), ""), member.companyId());
    }

    @PostMapping("/invites/accept")
    @Transactional(transactionManager = "empresasTransactionManager")
    public java.util.Map<String, String> acceptInvite(@AuthenticationPrincipal String email, @RequestBody java.util.Map<String, String> input) {
        String token = input == null ? "" : clean(input.get("token"));
        List<Object[]> invites = jdbc.query("SELECT id,company_id,role,email FROM company_invites WHERE token_hash=? AND accepted_at IS NULL AND expires_at>CURRENT_TIMESTAMP", (rs, row) -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)}, sha256(token));
        if (invites.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este convite expirou ou já foi utilizado.");
        Object[] invite = invites.get(0);
        if (!email.equalsIgnoreCase((String) invite[3])) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Entre com o e-mail informado no convite.");
        jdbc.update("INSERT INTO company_members(company_id,user_id,role) SELECT ?,id,? FROM company_users WHERE email=? ON CONFLICT(company_id,user_id) DO NOTHING", invite[1], invite[2], email);
        jdbc.update("UPDATE company_invites SET accepted_at=CURRENT_TIMESTAMP WHERE id=?", invite[0]);
        return java.util.Map.of("accountType", "COMPANY", "role", (String) invite[2]);
    }

    private String listingSql() { return "SELECT l.id,l.kind,l.title,l.area,l.description,l.requirements,l.location,l.modality,l.url,l.contact_email,l.salary,l.contract_type,l.work_schedule,l.workload,l.prerequisites,l.benefits,l.course_price,l.certificate,l.expires_at,l.price_cents,l.payment_status,l.publication_status,l.created_at,l.updated_at,(SELECT COUNT(*) FROM company_listing_events e WHERE e.listing_id=l.id AND e.event_type='LISTING_VIEW'),(SELECT COUNT(*) FROM company_listing_events e WHERE e.listing_id=l.id AND e.event_type='APPLICATION_CLICK'),(SELECT COUNT(*) FROM company_applications a WHERE a.listing_id=l.id),l.is_internship,l.accepted_courses,l.headcount,l.application_deadline,(SELECT p.checkout_url FROM company_listing_payments p WHERE p.listing_id=l.id ORDER BY p.created_at DESC LIMIT 1) FROM company_listings l"; }
    private org.springframework.jdbc.core.RowMapper<ListingView> listingMapper() { return (rs, row) -> new ListingView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14), rs.getString(15), rs.getString(16), rs.getString(17), rs.getString(18), rs.getDate(19) == null ? null : rs.getDate(19).toLocalDate().toString(), rs.getInt(20), rs.getString(21), rs.getString(22), rs.getTimestamp(23).toInstant().toString(), rs.getTimestamp(24).toInstant().toString(), rs.getLong(25), rs.getLong(26), rs.getLong(27), rs.getBoolean(28), rs.getString(29), (Integer) rs.getObject(30), rs.getDate(31) == null ? null : rs.getDate(31).toLocalDate().toString(), rs.getString(32)); }
    private ListingView listing(long companyId, long id) { return jdbc.queryForObject(listingSql() + " WHERE l.company_id=? AND l.id=?", listingMapper(), companyId, id); }
    private void ownListing(long companyId, long id) { if (jdbc.queryForObject("SELECT COUNT(*) FROM company_listings WHERE id=? AND company_id=?", Integer.class, id, companyId) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado."); }
    private Membership requireCompany(String email) {
        // O titular da conta da empresa é sempre OWNER, mesmo se o vínculo de equipe estiver ausente.
        List<Membership> owners = jdbc.query("SELECT c.id, 'OWNER' FROM companies c JOIN company_users u ON u.id=c.owner_user_id WHERE LOWER(u.email)=LOWER(?)", (rs, row) -> new Membership(rs.getLong(1), rs.getString(2)), email);
        if (!owners.isEmpty()) { requireActiveCompany(owners.get(0).companyId()); return owners.get(0); }
        List<Membership> memberships = jdbc.query("SELECT m.company_id,m.role FROM company_members m JOIN company_users u ON u.id=m.user_id WHERE LOWER(u.email)=LOWER(?) ORDER BY CASE m.role WHEN 'OWNER' THEN 0 ELSE 1 END", (rs, row) -> new Membership(rs.getLong(1), rs.getString(2)), email);
        if (memberships.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta área é exclusiva para contas empresariais.");
        requireActiveCompany(memberships.get(0).companyId());
        return memberships.get(0);
    }
    private void requireActiveCompany(long companyId) {
        Integer active = jdbc.queryForObject("SELECT COUNT(*) FROM companies WHERE id=? AND verification_status<>'SUSPENDED'", Integer.class, companyId);
        if (active == null || active == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A organização está suspensa. Contate a administração da Trampolim.");
    }
    private boolean companyApproved(long companyId) {
        return jdbc.queryForObject("SELECT verification_status='APPROVED' FROM companies WHERE id=?", Boolean.class, companyId);
    }
    private void requireRole(Membership membership, String... roles) { if (!Set.of(roles).contains(membership.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta ação."); }
    private void validate(ListingInput i) {
        if (i == null || !Set.of("job", "course").contains(clean(i.kind())) || blank(i.title()) || blank(i.area()) || blank(i.description()) || blank(i.requirements()) || blank(i.location()) || blank(i.modality()) || blank(i.contactEmail())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preencha todos os campos obrigatórios da divulgação.");
        if (!i.contactEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail de contato válido.");
        if (!Set.of("Presencial", "Híbrido", "Remoto", "Online").contains(i.modality())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma modalidade válida.");
        validateOptionalUrl(i.url());
        if (i.kind().equals("job") && !i.isInternship() && (blank(i.contractType()) || blank(i.workSchedule()) || blank(i.salary()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para vagas de emprego, informe salário/faixa, tipo de contrato e jornada.");
        if (i.kind().equals("course") && (blank(i.workload()) || blank(i.prerequisites()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para cursos, informe carga horária e pré-requisitos (ou escreva 'Sem pré-requisitos').");
        if (i.isInternship() && (!"job".equals(i.kind()) || (i.headcount() != null && (i.headcount() <= 0 || i.headcount() > 999)) || (i.acceptedCourses() != null && i.acceptedCourses().length() > 1000))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revise os dados da vaga de estágio.");
        if (i.isInternship() && i.applicationDeadline() != null && i.applicationDeadline().isBefore(today())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O prazo da vaga de estágio deve estar no futuro.");
    }
    private void validateOptionalUrl(String value) { if (blank(value)) return; try { URI uri = URI.create(value.trim()); if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) throw new IllegalArgumentException(); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um link válido começando com http:// ou https://."); } }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
    private LocalDate today() { return LocalDate.now(ZoneId.of("America/Sao_Paulo")); }
    private String clean(String s) { return s == null ? "" : s.trim(); }
    private String limit(String s, int max) { String value = clean(s); if (value.length() > max) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A apresentação pública deve ter até " + max + " caracteres."); return value; }
    private boolean blank(String s) { return clean(s).isBlank(); }
}
