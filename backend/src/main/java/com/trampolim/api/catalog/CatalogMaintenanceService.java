package com.trampolim.api.catalog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogMaintenanceService {
    private final CatalogItemRepository items;
    private final CatalogMetricRepository metrics;

    @Scheduled(cron = "0 15 8 * * *", zone = "America/Sao_Paulo")
    public void flagListingsForReview() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Instant now = Instant.now();
        items.findAll().stream().filter(CatalogItem::isActive).filter(item -> "job".equals(item.getKind()))
                .filter(item -> item.getExpiresAt() != null && !item.getExpiresAt().isAfter(today.plusDays(7)))
                .filter(item -> item.getLastValidityReminderAt() == null || item.getLastValidityReminderAt().isBefore(Instant.now().minusSeconds(6 * 86400)))
                .forEach(item -> {
                    item.setLastValidityReminderAt(Instant.now());
                    if (item.getExpiresAt().isBefore(today)) item.setActive(false);
                    items.save(item);
                    log.info("catalog_review_due itemId={} expired={}", item.getId(), item.getExpiresAt().isBefore(today));
                });
        items.findAll().stream().filter(CatalogItem::isActive)
                .filter(item -> item.getLastLinkCheckAt() == null || item.getLastLinkCheckAt().isBefore(now.minus(30, java.time.temporal.ChronoUnit.DAYS)))
                .filter(item -> item.getLastLinkReminderAt() == null || item.getLastLinkReminderAt().isBefore(now.minus(30, java.time.temporal.ChronoUnit.DAYS)))
                .forEach(item -> {
                    item.setLastLinkReminderAt(now); items.save(item);
                    log.info("catalog_link_check_due itemId={} lastCheckedAt={}", item.getId(), item.getLastLinkCheckAt());
                });
        metrics.deleteOlderThan(Instant.now().minusSeconds(400 * 86400));
    }
}
