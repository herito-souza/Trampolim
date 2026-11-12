package com.trampolim.api.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    @org.springframework.data.jpa.repository.Query("SELECT new com.trampolim.api.user.SessionAccount(u.id,u.email,u.tokenVersion) FROM AppUser u WHERE u.email=:email")
    Optional<SessionAccount> findSessionByEmail(@org.springframework.data.repository.query.Param("email") String email);
    Optional<AppUser> findByPasswordResetTokenHash(String passwordResetTokenHash);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from AppUser u where u.passwordResetTokenHash = :tokenHash")
    Optional<AppUser> findResetAccountForUpdate(@org.springframework.data.repository.query.Param("tokenHash") String tokenHash);
    Optional<AppUser> findByEmailVerificationTokenHash(String emailVerificationTokenHash);
    Optional<AppUser> findByPublicShareIdAndPublicProfileEnabledTrue(String publicShareId);
    java.util.List<AppUser> findAllByAlertPreferencesIsNotNull();
    boolean existsByEmail(String email);
}
