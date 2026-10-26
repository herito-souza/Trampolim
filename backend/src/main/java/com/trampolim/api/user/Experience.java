package com.trampolim.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
public class Experience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 140)
    private String position;

    @Column(nullable = false, length = 140)
    private String company;

    @Column(length = 7)
    private String startDate;

    @Column(length = 7)
    private String endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(length = 2000)
    private String description;
}
