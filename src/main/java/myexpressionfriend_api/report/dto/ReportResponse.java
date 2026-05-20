package myexpressionfriend_api.report.dto;

import myexpressionfriend_api.report.domain.AiReport;
import myexpressionfriend_api.report.domain.ReportStatus;
import myexpressionfriend_api.report.domain.ReportType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponse(
        UUID reportId,
        UUID childId,
        UUID generatedBy,
        UUID reviewedBy,
        ReportType reportType,
        ReportStatus status,
        String title,
        String content,
        String model,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime reviewedAt,
        LocalDateTime publishedAt,
        String ragContextSnapshot,
        String promptSnapshot
) {
    public static ReportResponse from(AiReport report, boolean includeDebugSnapshots) {
        return new ReportResponse(
                report.getReportId(),
                report.getChild().getChildId(),
                report.getGeneratedBy().getUserId(),
                report.getReviewedBy() == null ? null : report.getReviewedBy().getUserId(),
                report.getReportType(),
                report.getStatus(),
                report.getTitle(),
                report.getContent(),
                report.getModel(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getReviewedAt(),
                report.getPublishedAt(),
                includeDebugSnapshots ? report.getRagContextSnapshot() : null,
                includeDebugSnapshots ? report.getPromptSnapshot() : null
        );
    }
}
