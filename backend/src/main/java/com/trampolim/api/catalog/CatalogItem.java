package com.trampolim.api.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_items")
@Getter @Setter @NoArgsConstructor
public class CatalogItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 16) private String kind;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 180) private String provider;
    @Column(nullable = false, length = 3000) private String description;
    @Column(nullable = false, length = 1000) private String url;
    @Column(nullable = false, length = 32) private String area;
    @Column(length = 180) private String location;
    private Double latitude;
    private Double longitude;
    @Column(length = 40) private String modality;
    @Column(length = 40) private String experienceLevel;
    @Column(length = 40) private String contractType;
    @Column(length = 80) private String workSchedule;
    @Column(length = 80) private String salary;
    @Column(length = 2000) private String accessibility;
    private boolean active = true;
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'APPROVED'") private String moderationStatus = "APPROVED";
    @Column(length = 180) private String createdBy;
    @Column(length = 180) private String updatedBy;
    @Column(length = 180) private String reviewedBy;
    private Instant reviewedAt;
    private Instant publishedAt;
    private LocalDate expiresAt;
    private Instant lastValidityReminderAt;
    private Instant lastLinkCheckAt;
    private Instant lastLinkReminderAt;
    @Column(length = 20) private String linkStatus = "UNKNOWN";
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(columnDefinition = "timestamp with time zone default current_timestamp") private Instant updatedAt = Instant.now();
}
