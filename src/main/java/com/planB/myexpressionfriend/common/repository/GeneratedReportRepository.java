package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.report.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, UUID> {

    Optional<GeneratedReport> findByReportIdAndUserId(UUID reportId, UUID userId);

    Page<GeneratedReport> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, ReportStatus status);

    Page<GeneratedReport> findByStatusAndCreatedAtAfter(
            ReportStatus status,
            LocalDateTime createdAfter,
            Pageable pageable
    );
}
