package myexpressionfriend_api.report.repository;

import myexpressionfriend_api.report.domain.AiReport;
import myexpressionfriend_api.report.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiReportRepository extends JpaRepository<AiReport, UUID> {

    Page<AiReport> findByChild_ChildIdOrderByCreatedAtDesc(UUID childId, Pageable pageable);

    Page<AiReport> findByChild_ChildIdAndStatusOrderByCreatedAtDesc(
            UUID childId, ReportStatus status, Pageable pageable);

    Optional<AiReport> findByReportIdAndChild_ChildId(UUID reportId, UUID childId);

    Optional<AiReport> findByReportIdAndChild_ChildIdAndStatus(
            UUID reportId, UUID childId, ReportStatus status);
}
