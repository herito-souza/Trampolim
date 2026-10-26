package com.trampolim.api.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findAllByKindAndActiveTrueOrderByCreatedAtDesc(String kind);
    List<CatalogItem> findAllByKindOrderByCreatedAtDesc(String kind);
}
