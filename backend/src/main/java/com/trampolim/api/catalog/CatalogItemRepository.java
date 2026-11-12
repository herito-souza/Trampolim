package com.trampolim.api.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    @Query("SELECT c FROM CatalogItem c WHERE c.kind=:kind AND c.active=true AND c.moderationStatus='APPROVED' AND (c.expiresAt IS NULL OR c.expiresAt>=:today) ORDER BY c.createdAt DESC,c.id DESC")
    List<CatalogItem> findPublic(@Param("kind") String kind,@Param("today") LocalDate today,Pageable page);
    List<CatalogItem> findByKindOrderByCreatedAtDescIdDesc(String kind,Pageable page);
    List<CatalogItem> findAllByKindAndActiveTrueOrderByCreatedAtDesc(String kind);
    List<CatalogItem> findAllByKindOrderByCreatedAtDesc(String kind);
}
