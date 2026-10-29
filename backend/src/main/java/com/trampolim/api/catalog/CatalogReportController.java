package com.trampolim.api.catalog;

import com.trampolim.api.user.AppUserRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.trampolim.api.security.PlatformAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CatalogReportController {
    private final CatalogReportRepository reports;
    private final CatalogItemRepository items;
    private final AppUserRepository users;
    private final PlatformAccess platformAccess;

    public record ReportInput(String reason, String details) {}
    public record ReportData(Long id, Long itemId, String itemTitle, String kind, String reason, String details, String status, Instant createdAt) {}
    public record ReportStatusInput(String status) {}

    @PostMapping("/opportunities/{kind}/{id}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthenticationPrincipal String email, @PathVariable String kind, @PathVariable Long id, @RequestBody ReportInput input) {
        if (!List.of("course", "job").contains(kind)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (input == null || !List.of("EXPIRED", "BROKEN_LINK", "MISLEADING", "DUPLICATE", "OTHER").contains(input.reason()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um motivo válido para o aviso.");
        String details = input.details() == null ? null : input.details().trim();
        if (details != null && details.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comentário deve ter até 500 caracteres.");
        var reporter = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var item = items.findById(id).filter(CatalogItem::isActive).filter(value -> "APPROVED".equals(value.getModerationStatus())).filter(value -> kind.equals(value.getKind()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta publicação não está disponível."));
        if (reports.existsByItem_IdAndReporter_IdAndCreatedAtAfter(id, reporter.getId(), Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS))) return;
        CatalogReport report = new CatalogReport(); report.setItem(item); report.setReporter(reporter); report.setReason(input.reason());
        report.setDetails(details == null || details.isBlank() ? null : details); reports.save(report);
        log.info("catalog_report_received reportId={} itemId={} reason={}", report.getId(), id, input.reason());
    }

    @GetMapping("/admin/catalog-reports")
    public List<ReportData> listOpen(@AuthenticationPrincipal String email) {
        requireAdmin(email);
        return reports.findAllByStatusOrderByCreatedAtAsc("OPEN").stream().map(this::toData).toList();
    }

    @PutMapping("/admin/catalog-reports/{id}")
    public ReportData review(@AuthenticationPrincipal String email, @PathVariable Long id, @RequestBody ReportStatusInput input) {
        requireAdmin(email);
        if (input == null || !List.of("RESOLVED", "DISMISSED").contains(input.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha se o aviso foi resolvido ou descartado.");
        CatalogReport report = reports.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        report.setStatus(input.status()); report.setReviewedAt(Instant.now()); report.setReviewedBy(email);
        CatalogReport saved = reports.save(report);
        log.info("catalog_report_reviewed reportId={} status={}", id, input.status());
        return toData(saved);
    }

    private ReportData toData(CatalogReport report) {
        return new ReportData(report.getId(), report.getItem().getId(), report.getItem().getTitle(), report.getItem().getKind(),
                report.getReason(), report.getDetails(), report.getStatus(), report.getCreatedAt());
    }
    private void requireAdmin(String email) {
        if (!platformAccess.isSuperAdmin(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito à administração.");
    }
}
