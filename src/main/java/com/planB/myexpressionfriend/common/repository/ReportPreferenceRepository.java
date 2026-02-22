package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportPreferenceRepository extends JpaRepository<ReportPreference, UUID> {

    Optional<ReportPreference> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<ReportPreference> findByEnabledTrueAndNextIssueAtLessThanEqual(LocalDateTime now);
}
