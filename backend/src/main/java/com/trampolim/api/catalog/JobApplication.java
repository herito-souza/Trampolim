package com.trampolim.api.catalog;

import com.trampolim.api.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_applications", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"}))
@Getter @Setter @NoArgsConstructor
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private AppUser user;
    @ManyToOne(optional = false) @JoinColumn(name = "item_id") private CatalogItem item;
    @Column(nullable = false, length = 24) private String status = "SAVED";
    @Column(nullable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();
    @Column(length = 2000) private String notes;
    private LocalDate followUpDate;
    private Instant reminderSentAt;
}
