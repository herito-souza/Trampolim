package com.trampolim.api.company;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/companies")
@RequiredArgsConstructor
public class PublicCompanyController {
    private final JdbcTemplate jdbc;
    private final ConcurrentHashMap<String, RateWindow> rateLimits = new ConcurrentHashMap<>();

    public record ApplicationInput(String name, String email, String phone, String message, boolean consent) {}
    public record PublicListing(long id, String kind, String title, String area, String description, String requirements,
            String location, String modality, String url, String salary, String contractType, String workSchedule,
            String workload, String prerequisites, String benefits, String coursePrice, String certificate, String expiresAt) {}
    public record PublicCompany(String publicSlug, String tradeName, String industry, String city, String state,
            String website, String description, List<PublicListing> listings) {}
    private record RateWindow(Instant resetAt, int count) {}

    @GetMapping("/{slug}")
    public PublicCompany profile(@PathVariable String slug) {
        var companies = jdbc.query("SELECT id, public_slug, trade_name, industry, city, state, website, profile_description FROM companies WHERE public_slug=?", (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)}, slug);
        if (companies.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta página de empresa não está disponível.");
        Object[] company = companies.get(0); long companyId = (long) company[0];
        List<PublicListing> listings = jdbc.query("SELECT id,kind,title,area,description,requirements,location,modality,url,salary,contract_type,work_schedule,workload,prerequisites,benefits,course_price,certificate,expires_at FROM company_listings WHERE company_id=? AND publication_status='PUBLISHED' AND (expires_at IS NULL OR expires_at>=CURRENT_DATE) ORDER BY created_at DESC",
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
        if (input == null || blank(input.name())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome: preencha este campo.");
        if (input.email() == null || !input.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail: informe um endereço válido.");
        if (!input.consent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Autorize o compartilhamento dos dados com a empresa para enviar sua candidatura.");
        if (input.message() != null && input.message().length() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A mensagem deve ter até 2.000 caracteres.");
        try {
            jdbc.update("INSERT INTO company_applications(listing_id,candidate_name,candidate_email,candidate_phone,message) VALUES (?,?,?,?,?)", listing, input.name().trim(), input.email().trim().toLowerCase(), clean(input.phone()), clean(input.message()));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já enviou uma candidatura para esta oportunidade.");
        }
        return Map.of("message", "Sua candidatura foi enviada à empresa.");
    }

    private long publishedListing(String slug, long id) {
        List<Long> ids = jdbc.query("SELECT l.id FROM company_listings l JOIN companies c ON c.id=l.company_id WHERE c.public_slug=? AND l.id=? AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE)", (rs, row) -> rs.getLong(1), slug, id);
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
