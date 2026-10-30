package com.trampolim.api.company;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/companies")
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class PublicCompanyController {
    private final JdbcTemplate jdbc;
    private final ConcurrentHashMap<String, RateWindow> rateLimits = new ConcurrentHashMap<>();

    public PublicCompanyController(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record ApplicationInput(String name, String email, String phone, String message, boolean consent) {}
    public record PublicListing(long id, String kind, String title, String area, String description, String requirements,
            String location, String modality, String url, String salary, String contractType, String workSchedule,
            String workload, String prerequisites, String benefits, String coursePrice, String certificate, String expiresAt) {}
    public record PublicCompany(String publicSlug, String tradeName, String industry, String city, String state,
            String website, String description, List<PublicListing> listings) {}
    public record PublicInternship(long id, String slug, String companyName, String title, String area, String description, String requirements,
            String acceptedCourses, String location, String modality, String workSchedule, String workload, String salary, String benefits,
            Integer quantity, String expiresAt) {}
    private record RateWindow(Instant resetAt, int count) {}

    public record PublicOpportunity(long id, String companySlug, String companyName, String title,
            String area, String description, String requirements, String location, String modality,
            String salary, String contractType, String expiresAt, boolean internship) {}

    @GetMapping("/opportunities")
    public List<PublicOpportunity> opportunities() {
        return jdbc.query("""
                SELECT l.id,c.public_slug,c.trade_name,l.title,l.area,l.description,l.requirements,l.location,
                       l.modality,l.salary,l.contract_type,l.expires_at,l.is_internship
                FROM company_listings l JOIN companies c ON c.id=l.company_id
                WHERE c.verification_status='APPROVED' AND l.kind='job' AND l.publication_status='PUBLISHED'
                  AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE)
                  AND (l.application_deadline IS NULL OR l.application_deadline>=CURRENT_DATE)
                  AND (l.is_internship=FALSE OR l.payment_status='PAID')
                ORDER BY l.created_at DESC,l.id DESC
                """, (rs,row) -> new PublicOpportunity(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),
                rs.getString(11),rs.getDate(12)==null?null:rs.getDate(12).toLocalDate().toString(),rs.getBoolean(13)));
    }
    public List<PublicInternship> internships(String area,String location,String modality,String requirements) {return internships(area,location,modality,requirements,null,null);}
    @GetMapping("/internships")
    public List<PublicInternship> internships(@RequestParam(required = false) String area,
            @RequestParam(required = false) String location, @RequestParam(required = false) String modality,
            @RequestParam(required = false) String requirements,@RequestParam(required=false) Integer limit,@RequestParam(required=false) Long before) {
        if(limit!=null&&(limit<1||limit>200)||before!=null&&before<1||java.util.stream.Stream.of(area,location,modality,requirements).anyMatch(value->value!=null&&value.length()>120)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Consulta inválida.");
        StringBuilder sql = new StringBuilder("SELECT l.id,c.public_slug,c.trade_name,l.title,l.area,l.description,l.requirements,l.accepted_courses,l.location,l.modality,l.work_schedule,l.workload,l.salary,l.benefits,l.headcount,l.application_deadline FROM company_listings l JOIN companies c ON c.id=l.company_id WHERE c.verification_status='APPROVED' AND l.kind='job' AND l.is_internship=TRUE AND l.payment_status='PAID' AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE) AND (l.application_deadline IS NULL OR l.application_deadline>=CURRENT_DATE)");
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        if (area != null && !area.isBlank()) { sql.append(" AND LOWER(l.area) LIKE LOWER(?)"); parameters.add("%" + area.trim() + "%"); }
        if (location != null && !location.isBlank()) { sql.append(" AND LOWER(l.location) LIKE LOWER(?)"); parameters.add("%" + location.trim() + "%"); }
        if (modality != null && !modality.isBlank()) { sql.append(" AND l.modality=?"); parameters.add(modality.trim()); }
        if (requirements != null && !requirements.isBlank()) { sql.append(" AND (LOWER(l.requirements) LIKE LOWER(?) OR LOWER(COALESCE(l.accepted_courses,'')) LIKE LOWER(?))"); parameters.add("%" + requirements.trim() + "%"); parameters.add("%" + requirements.trim() + "%"); }
        if(before!=null){sql.append(" AND l.id<?");parameters.add(before);}
        sql.append(limit==null?" ORDER BY l.created_at DESC,l.id DESC":" ORDER BY l.id DESC");
        if(limit!=null){sql.append(" LIMIT ?");parameters.add(limit);}
        return jdbc.query(sql.toString(), (rs, row) -> new PublicInternship(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14), (Integer) rs.getObject(15), rs.getDate(16) == null ? null : rs.getDate(16).toLocalDate().toString()), parameters.toArray());
    }

    @GetMapping("/internships/{listingId}")
    public PublicInternship internship(@PathVariable long listingId) {
        return jdbc.query("SELECT l.id,c.public_slug,c.trade_name,l.title,l.area,l.description,l.requirements,l.accepted_courses,l.location,l.modality,l.work_schedule,l.workload,l.salary,l.benefits,l.headcount,l.application_deadline FROM company_listings l JOIN companies c ON c.id=l.company_id WHERE c.verification_status='APPROVED' AND l.id=? AND l.kind='job' AND l.is_internship=TRUE AND l.payment_status='PAID' AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE) AND (l.application_deadline IS NULL OR l.application_deadline>=CURRENT_DATE)", (rs, row) -> new PublicInternship(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14), (Integer) rs.getObject(15), rs.getDate(16) == null ? null : rs.getDate(16).toLocalDate().toString()), listingId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta vaga de estágio não está disponível."));
    }

    @GetMapping("/{slug}")
    public PublicCompany profile(@PathVariable String slug) {
        var companies = jdbc.query("SELECT c.id, c.public_slug, c.trade_name, c.industry, c.city, c.state, c.website, c.profile_description FROM companies c WHERE c.public_slug=? AND c.verification_status='APPROVED'", (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)}, slug);
        if (companies.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta página de empresa não está disponível.");
        Object[] company = companies.get(0); long companyId = (long) company[0];
        List<PublicListing> listings = jdbc.query("SELECT id,kind,title,area,description,requirements,location,modality,url,salary,contract_type,work_schedule,workload,prerequisites,benefits,course_price,certificate,expires_at FROM company_listings WHERE company_id=? AND publication_status='PUBLISHED' AND (expires_at IS NULL OR expires_at>=CURRENT_DATE) AND (application_deadline IS NULL OR application_deadline>=CURRENT_DATE) AND (is_internship=FALSE OR payment_status='PAID') ORDER BY created_at DESC",
                (rs, row) -> new PublicListing(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14), rs.getString(15), rs.getString(16), rs.getString(17), rs.getDate(18) == null ? null : rs.getDate(18).toLocalDate().toString()), companyId);
        return new PublicCompany((String) company[1], (String) company[2], (String) company[3], (String) company[4], (String) company[5], (String) company[6], (String) company[7], listings);
    }

    @PostMapping("/{slug}/listings/{listingId}/view")
    public Map<String, String> view(@PathVariable String slug, @PathVariable long listingId) {
        long listing = publishedListing(slug, listingId);
        jdbc.update("INSERT INTO company_listing_events(listing_id,event_type) VALUES (?, 'LISTING_VIEW')", listing);
        return Map.of("message", "Visualização registrada.");
    }

    @PostMapping("/{slug}/listings/{listingId}/click")
    public Map<String, String> click(@PathVariable String slug, @PathVariable long listingId) {
        long listing = publishedListing(slug, listingId);
        jdbc.update("INSERT INTO company_listing_events(listing_id,event_type) VALUES (?, 'APPLICATION_CLICK')", listing);
        return Map.of("message", "Interesse registrado.");
    }

    @PostMapping("/{slug}/listings/{listingId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> apply(@PathVariable String slug, @PathVariable long listingId, @RequestBody ApplicationInput input, jakarta.servlet.http.HttpServletRequest request) {
        limitRequests(request.getRemoteAddr());
        long listing = publishedListing(slug, listingId);
        if (input == null || blank(input.name()) || input.name().trim().length() > 120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome: preencha este campo (até 120 caracteres).");
        if (input.email() == null || input.email().trim().length() > 180 || !input.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail: informe um endereço válido.");
        if (!input.consent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Autorize o compartilhamento dos dados com a empresa para enviar sua candidatura.");
        if (input.phone() != null && input.phone().length() > 40) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O telefone deve ter até 40 caracteres.");
        if (input.message() != null && input.message().length() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A mensagem deve ter até 2.000 caracteres.");
        try {
            jdbc.update("INSERT INTO company_applications(listing_id,candidate_name,candidate_email,candidate_phone,message) VALUES (?,?,?,?,?)", listing, input.name().trim(), input.email().trim().toLowerCase(), clean(input.phone()), clean(input.message()));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já enviou uma candidatura para esta oportunidade.");
        }
        return Map.of("message", "Sua candidatura foi enviada à empresa.");
    }

    private long publishedListing(String slug, long id) {
        List<Long> ids = jdbc.query("SELECT l.id FROM company_listings l JOIN companies c ON c.id=l.company_id WHERE c.public_slug=? AND c.verification_status='APPROVED' AND l.id=? AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE) AND (l.application_deadline IS NULL OR l.application_deadline>=CURRENT_DATE) AND (l.is_internship=FALSE OR l.payment_status='PAID')", (rs, row) -> rs.getLong(1), slug, id);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta oportunidade não está mais disponível.");
        return ids.get(0);
    }
    private void limitRequests(String address) {
        Instant now = Instant.now();
        RateWindow window = rateLimits.compute(address, (key, old) -> old == null || old.resetAt().isBefore(now) ? new RateWindow(now.plusSeconds(60), 1) : new RateWindow(old.resetAt(), old.count() + 1));
        if (rateLimits.size() > 10_000) rateLimits.entrySet().removeIf(entry -> entry.getValue().resetAt().isBefore(now));
        if (window.count() > 10) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas candidaturas foram enviadas. Tente novamente em um minuto.");
    }
    private boolean blank(String s) { return clean(s).isBlank(); }
    private String clean(String s) { return s == null ? "" : s.trim(); }
}
