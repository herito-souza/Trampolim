package com.trampolim.api.catalog;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CatalogMetricRepository extends JpaRepository<CatalogMetric, Long> {
    interface MetricSummary { String getDay(); String getKind(); String getEventType(); String getItemTitle(); Long getTotal(); }

    @Query(value = "select cast(cast(metric.created_at as date) as text) as day, metric.kind, metric.event_type as \"eventType\", " +
            "coalesce(item.title, metric.item_key) as \"itemTitle\", count(*) as total from catalog_metrics metric " +
            "left join catalog_items item on cast(item.id as text) = metric.item_key where metric.created_at >= :fromDate " +
            "group by cast(metric.created_at as date), metric.kind, metric.event_type, item.title, metric.item_key order by cast(metric.created_at as date)", nativeQuery = true)
    List<MetricSummary> summarizeSince(@Param("fromDate") Instant fromDate);

    @Modifying
    @Transactional
    @Query("delete from CatalogMetric metric where metric.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
