package com.trampolim.api.catalog;

import java.util.Arrays;
import java.util.List;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CatalogController {
    private final CatalogItemRepository items;
    private final JobApplicationRepository applications;
    private final OpportunityAlertService alerts;
    @Value("${app.admin.emails:}") private String adminEmails;

    public record ItemInput(String kind, String title, String provider, String description, String url, String area,
                            String location, String modality, String experienceLevel, String salary, String accessibility,
                            String contractType, String workSchedule, Double latitude, Double longitude, boolean active, LocalDate expiresAt) {}
    public record ReviewInput(String status) {}
    public record VisibilityInput(boolean active) {}
    public record LinkCheckInput(boolean reachable) {}
    public record PublicCatalogItem(Long id, String kind, String title, String provider, String description, String url, String area,
                                    String location, String modality, String experienceLevel, String salary, String accessibility, String contractType, String workSchedule, Double latitude, Double longitude,
                                    boolean active, LocalDate expiresAt, Instant createdAt, Instant publishedAt, String linkStatus, Instant lastLinkCheckAt) {}

    @GetMapping("/opportunities/{kind}")
    public List<PublicCatalogItem> list(@PathVariable String kind) {
        validateKind(kind);
        return items.findAllByKindAndActiveTrueOrderByCreatedAtDesc(kind).stream()
                .filter(item -> "APPROVED".equals(item.getModerationStatus()))
                .filter(item -> item.getExpiresAt() == null || !item.getExpiresAt().isBefore(todayBrazil()))
                .map(item -> new PublicCatalogItem(item.getId(), item.getKind(), item.getTitle(), item.getProvider(), item.getDescription(), item.getUrl(), item.getArea(), item.getLocation(), item.getModality(), item.getExperienceLevel(), item.getSalary(), item.getAccessibility(), item.getContractType(), item.getWorkSchedule(), item.getLatitude(), item.getLongitude(), item.isActive(), item.getExpiresAt(), item.getCreatedAt(), item.getPublishedAt(), item.getLinkStatus(), item.getLastLinkCheckAt()))
                .toList();
    }

    @GetMapping("/admin/catalog/{kind}")
    public List<CatalogItem> adminList(@AuthenticationPrincipal String email, @PathVariable String kind) {
        requireAdmin(email); validateKind(kind);
        return items.findAllByKindOrderByCreatedAtDesc(kind);
    }

    @PostMapping("/admin/catalog")
    public CatalogItem create(@AuthenticationPrincipal String email, @RequestBody ItemInput input) {
        requireAdmin(email); validate(input);
        CatalogItem item = new CatalogItem(); apply(item, input); item.setCreatedBy(email); item.setUpdatedBy(email);
        item.setModerationStatus("PENDING"); item.setActive(false);
        CatalogItem saved = items.save(item); log.info("catalog_created itemId={} kind={}", saved.getId(), saved.getKind());
        return saved;
    }

    @PutMapping("/admin/catalog/{id}")
    public CatalogItem update(@AuthenticationPrincipal String email, @PathVariable Long id, @RequestBody ItemInput input) {
        requireAdmin(email); validate(input);
        CatalogItem item = items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        apply(item, input); item.setUpdatedBy(email); item.setModerationStatus("PENDING"); item.setActive(false);
        item.setReviewedAt(null); item.setReviewedBy(null); item.setUpdatedAt(Instant.now());
        CatalogItem saved = items.save(item); log.info("catalog_updated itemId={} reviewRequired=true", saved.getId());
        return saved;
    }

    @PutMapping("/admin/catalog/{id}/review")
    public CatalogItem review(@AuthenticationPrincipal String email, @PathVariable Long id, @RequestBody ReviewInput input) {
        requireAdmin(email);
        if (input == null || !List.of("APPROVED", "REJECTED").contains(input.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha aprovar ou rejeitar a publicação.");
        CatalogItem item = items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.setModerationStatus(input.status()); item.setReviewedBy(email); item.setReviewedAt(Instant.now()); item.setUpdatedAt(Instant.now());
        item.setLastValidityReminderAt(null);
        if ("APPROVED".equals(input.status()) && item.getExpiresAt() != null && item.getExpiresAt().isBefore(todayBrazil()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atualize o prazo da publicação antes de aprová-la.");
        if ("APPROVED".equals(input.status()) && "BROKEN".equals(item.getLinkStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirme que o link voltou a funcionar antes de aprovar a publicação.");
        item.setActive("APPROVED".equals(input.status()));
        if (item.isActive()) item.setPublishedAt(Instant.now());
        CatalogItem saved = items.save(item);
        log.info("catalog_review itemId={} status={}", saved.getId(), saved.getModerationStatus());
        if (saved.isActive()) alerts.published(saved);
        return saved;
    }

    @PutMapping("/admin/catalog/{id}/visibility")
    public CatalogItem setVisibility(@AuthenticationPrincipal String email, @PathVariable Long id, @RequestBody VisibilityInput input) {
        requireAdmin(email);
        CatalogItem item = items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (input.active() && (!"APPROVED".equals(item.getModerationStatus()) || "BROKEN".equals(item.getLinkStatus()) || (item.getExpiresAt() != null && item.getExpiresAt().isBefore(todayBrazil()))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas publicações aprovadas, com link ativo e dentro do prazo podem ser reativadas.");
        item.setActive(input.active()); item.setUpdatedBy(email); item.setUpdatedAt(Instant.now());
        CatalogItem saved = items.save(item); log.info("catalog_visibility itemId={} active={}", saved.getId(), saved.isActive());
        return saved;
    }

    @PostMapping("/admin/catalog/{id}/link-check")
    public CatalogItem recordLinkCheck(@AuthenticationPrincipal String email, @PathVariable Long id, @RequestBody LinkCheckInput input) {
        requireAdmin(email);
        if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o resultado da conferência.");
        CatalogItem item = items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.setLastLinkCheckAt(Instant.now()); item.setLastLinkReminderAt(null); item.setLinkStatus(input.reachable() ? "ACTIVE" : "BROKEN");
        if (!input.reachable()) item.setActive(false);
        item.setUpdatedBy(email); item.setUpdatedAt(Instant.now());
        log.info("catalog_link_review itemId={} reachable={}", id, input.reachable());
        return items.save(item);
    }

    @DeleteMapping("/admin/catalog/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String email, @PathVariable Long id) {
        requireAdmin(email);
        CatalogItem item = items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean archived = applications.existsByItemId(id);
        if (archived) { item.setActive(false); item.setUpdatedBy(email); item.setUpdatedAt(Instant.now()); items.save(item); }
        else items.delete(item);
        log.info("catalog_removed itemId={} archived={}", id, archived);
    }

    private void apply(CatalogItem item, ItemInput input) {
        item.setKind(input.kind()); item.setTitle(input.title().trim()); item.setProvider(input.provider().trim());
        item.setDescription(input.description().trim()); item.setUrl(input.url().trim()); item.setArea(input.area());
        item.setLocation(clean(input.location())); item.setModality(clean(input.modality()));
        item.setExperienceLevel(clean(input.experienceLevel())); item.setSalary(clean(input.salary()));
        item.setContractType(clean(input.contractType())); item.setWorkSchedule(clean(input.workSchedule()));
        item.setLatitude(input.latitude()); item.setLongitude(input.longitude());
        item.setAccessibility(clean(input.accessibility())); item.setExpiresAt(input.expiresAt());
        item.setLastValidityReminderAt(null);
        item.setActive(false); item.setUpdatedAt(Instant.now());
    }
    private void validate(ItemInput input) {
        if (input == null || !List.of("course", "job").contains(input.kind()) || blank(input.title()) || blank(input.provider())
                || blank(input.description()) || blank(input.url()) || blank(input.area()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preencha tipo, título, responsável, descrição, link e área.");
        try {
            java.net.URI uri = java.net.URI.create(input.url().trim());
            String scheme = uri.getScheme();
            if (( !"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) || uri.getHost() == null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O link precisa começar com http:// ou https://.");
        }
        if (!List.of("service", "sales", "admin", "numbers", "logistics", "maintenance", "production", "technology", "health", "marketing").contains(input.area()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma área válida do catálogo.");
        if ("job".equals(input.kind()) && input.expiresAt() != null && input.expiresAt().isBefore(todayBrazil()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data limite da vaga não pode estar no passado.");
        if ("job".equals(input.kind()) && input.contractType() != null && !input.contractType().isBlank() && !List.of("CLT", "PJ", "Temporário", "Estágio", "Aprendiz", "Autônomo").contains(input.contractType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um tipo de contrato válido.");
        if ((input.latitude() == null) != (input.longitude() == null) || (input.latitude() != null && (input.latitude() < -90 || input.latitude() > 90 || input.longitude() < -180 || input.longitude() > 180)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe latitude e longitude válidas ou deixe ambas em branco.");
    }
    private void validateKind(String kind) { if (!List.of("course", "job").contains(kind)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    private LocalDate todayBrazil() { return LocalDate.now(ZoneId.of("America/Sao_Paulo")); }
    private void requireAdmin(String email) {
        boolean allowed = Arrays.stream(adminEmails.split(",")).map(String::trim).anyMatch(value -> value.equalsIgnoreCase(email));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito à administração.");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String clean(String value) { return blank(value) ? null : value.trim(); }
}
