package com.trampolim.api.catalog;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogMetricController {
    private final CatalogMetricRepository metrics;
    private final CatalogItemRepository items;
    @Value("${app.admin.emails:}") private String adminEmails;
    private final ConcurrentHashMap<String, RateWindow> eventRates = new ConcurrentHashMap<>();
    private record RateWindow(Instant expiresAt, int count) {}

    public record MetricEvent(String eventType, String kind, String itemKey) {}
    public record MetricSummary(String day, String kind, String eventType, String itemTitle, Long total) {}

    @PostMapping("/metrics/catalog")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@AuthenticationPrincipal String email, @RequestBody MetricEvent input, HttpServletRequest request) {
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        limitEvents(request.getRemoteAddr());
        if (input == null || !List.of("course", "job").contains(input.kind()) || !List.of("open", "apply").contains(input.eventType())
                || input.itemKey() == null || input.itemKey().length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (input.itemKey().matches("\\d+")) {
            long itemId;
            try { itemId = Long.parseLong(input.itemKey()); }
            catch (NumberFormatException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
            CatalogItem item = items.findById(itemId).filter(CatalogItem::isActive)
                    .filter(candidate -> "APPROVED".equals(candidate.getModerationStatus()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!item.getKind().equals(input.kind())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        CatalogMetric metric = new CatalogMetric(); metric.setEventType(input.eventType()); metric.setKind(input.kind()); metric.setItemKey(input.itemKey());
        metrics.save(metric);
    }

    private void limitEvents(String remoteAddress) {
        Instant now = Instant.now();
        RateWindow current = eventRates.compute(remoteAddress, (key, previous) -> previous == null || previous.expiresAt().isBefore(now)
                ? new RateWindow(now.plus(1, ChronoUnit.HOURS), 1) : new RateWindow(previous.expiresAt(), previous.count() + 1));
        if (eventRates.size() > 10_000) eventRates.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        if (current.count() > 240) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de eventos temporariamente atingido.");
    }

    @GetMapping("/admin/metrics")
    public List<MetricSummary> summaries(@AuthenticationPrincipal String email) {
        boolean allowed = Arrays.stream(adminEmails.split(",")).map(String::trim).anyMatch(value -> value.equalsIgnoreCase(email));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito à administração.");
        return metrics.summarizeSince(Instant.now().minus(30, ChronoUnit.DAYS)).stream()
                .map(row -> new MetricSummary(row.getDay(), row.getKind(), row.getEventType(), row.getItemTitle(), row.getTotal())).toList();
    }
}
