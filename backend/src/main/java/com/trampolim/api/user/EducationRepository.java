package com.trampolim.api.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findAllByUserIdOrderByIdDesc(Long userId);
    Optional<Education> findByIdAndUserId(Long id, Long userId);
    void deleteAllByUserId(Long userId);
}
