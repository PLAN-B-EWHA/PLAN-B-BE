package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.report.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, UUID> {

    Optional<GeneratedReport> findByReportIdAndUserId(UUID reportId, UUID userId);

    Page<GeneratedReport> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            SELECT r FROM GeneratedReport r
            WHERE r.reportId = :reportId
              AND r.targetChildId IS NOT NULL
              AND EXISTS (
                SELECT 1 FROM ChildrenAuthorizedUser au
                WHERE au.child.childId = r.targetChildId
                  AND au.user.userId = :requesterUserId
                  AND au.isActive = true
                  AND (
                    au.isPrimary = true
                    OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
                  )
              )
            """)
    Optional<GeneratedReport> findAuthorizedByReportId(
            @Param("reportId") UUID reportId,
            @Param("requesterUserId") UUID requesterUserId
    );

    @Query("""
            SELECT r FROM GeneratedReport r
            WHERE r.targetChildId IS NOT NULL
              AND EXISTS (
                SELECT 1 FROM ChildrenAuthorizedUser au
                WHERE au.child.childId = r.targetChildId
                  AND au.user.userId = :requesterUserId
                  AND au.isActive = true
                  AND (
                    au.isPrimary = true
                    OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
                  )
              )
            ORDER BY r.createdAt DESC
            """)
    Page<GeneratedReport> findAuthorizedReports(
            @Param("requesterUserId") UUID requesterUserId,
            Pageable pageable
    );

    long countByUserIdAndStatus(UUID userId, ReportStatus status);

    Page<GeneratedReport> findByStatusAndCreatedAtAfter(
            ReportStatus status,
            LocalDateTime createdAfter,
            Pageable pageable
    );
}
