package com.trampolim.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified = true;

    @Column(length = 64)
    private String emailVerificationTokenHash;

    private Instant emailVerificationExpiresAt;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int failedLoginAttempts;

    private Instant loginLockedUntil;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int tokenVersion;

    private Instant lastLoginAt;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'PERSON'")
    private String accountType = "PERSON";

    @Column(length = 40)
    private String phone;

    @Column(length = 120)
    private String location;

    @Column(length = 2000)
    private String summary;

    @Column(columnDefinition = "text")
    private String diagnosticData;

    @Column(columnDefinition = "text")
    private String favoriteOpportunityIds;

    @Column(length = 64)
    private String passwordResetTokenHash;

    private Instant passwordResetExpiresAt;

    @Column(length = 36, unique = true)
    private String publicShareId;

    private boolean publicProfileEnabled;

    private boolean shareLocation;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean shareSummary = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean shareExperiences = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean shareEducation = true;

    @Column(columnDefinition = "text")
    private String alertPreferences;

    @Column(columnDefinition = "text")
    private String pendingAlertIds;

    @Column(length = 255)
    private String resumeFileName;

    @Column(length = 120)
    private String resumeContentType;

    @Column(columnDefinition = "bytea")
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] resumeContent;

    @Column(length = 255)
    private String photoFileName;

    @Column(length = 80)
    private String photoContentType;

    @Column(columnDefinition = "bytea")
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] photoContent;
}
