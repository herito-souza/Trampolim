package com.trampolim.api.catalog;

import com.trampolim.api.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_reports")
@Getter @Setter @NoArgsConstructor
public class CatalogReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "item_id") private CatalogItem item;
    @ManyToOne(optional = false) @JoinColumn(name = "reporter_user_id") private AppUser reporter;
    @Column(nullable = false, length = 32) private String reason;
    @Column(length = 500) private String details;
    @Column(nullable = false, length = 16) private String status = "OPEN";
    @Column(nullable = false) private Instant createdAt = Instant.now();
    private Instant reviewedAt;
    @Column(length = 180) private String reviewedBy;
}
