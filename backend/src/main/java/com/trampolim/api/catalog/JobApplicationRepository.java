package com.trampolim.api.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);
    Optional<JobApplication> findByUserIdAndItemId(Long userId, Long itemId);
    boolean existsByItemId(Long itemId);
    List<JobApplication> findAllByReminderSentAtIsNullAndFollowUpDateIsNotNull();
    void deleteAllByUserId(Long userId);
}
