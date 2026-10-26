package com.trampolim.api.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByPasswordResetTokenHash(String passwordResetTokenHash);
    Optional<AppUser> findByEmailVerificationTokenHash(String emailVerificationTokenHash);
    Optional<AppUser> findByPublicShareIdAndPublicProfileEnabledTrue(String publicShareId);
    java.util.List<AppUser> findAllByAlertPreferencesIsNotNull();
    boolean existsByEmail(String email);
}
