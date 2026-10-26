package com.trampolim.api.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogReportRepository extends JpaRepository<CatalogReport, Long> {
    boolean existsByItem_IdAndReporter_IdAndCreatedAtAfter(Long itemId, Long reporterId, java.time.Instant createdAt);
    List<CatalogReport> findAllByStatusOrderByCreatedAtAsc(String status);
}
