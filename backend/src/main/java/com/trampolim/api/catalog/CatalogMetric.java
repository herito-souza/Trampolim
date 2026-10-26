package com.trampolim.api.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_metrics")
@Getter @Setter @NoArgsConstructor
public class CatalogMetric {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 24) private String eventType;
    @Column(nullable = false, length = 16) private String kind;
    @Column(nullable = false, length = 100) private String itemKey;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
