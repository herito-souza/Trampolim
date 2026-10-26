package com.trampolim.api.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    List<Experience> findAllByUserIdOrderByIdDesc(Long userId);
    Optional<Experience> findByIdAndUserId(Long id, Long userId);
    void deleteAllByUserId(Long userId);
}
