package com.planB.myexpressionfriend.common.dto.report;

import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.report.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GeneratedReportDTO {

    private UUID reportId;
    private UUID userId;
    private UUID targetChildId;
    private UUID preferenceId;
    private ReportStatus status;
    private String title;
    private String summary;
    private String reportBody;
    private String promptUsed;
    private String modelName;
    private LocalDateTime periodStartAt;
    private LocalDateTime periodEndAt;
    private LocalDateTime issuedAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GeneratedReportDTO from(GeneratedReport report) {
        return GeneratedReportDTO.builder()
                .reportId(report.getReportId())
                .userId(report.getUserId())
                .targetChildId(report.getTargetChildId())
                .preferenceId(report.getPreferenceId())
                .status(report.getStatus())
                .title(report.getTitle())
                .summary(report.getSummary())
                .reportBody(report.getReportBody())
                .promptUsed(report.getPromptUsed())
                .modelName(report.getModelName())
                .periodStartAt(report.getPeriodStartAt())
                .periodEndAt(report.getPeriodEndAt())
                .issuedAt(report.getIssuedAt())
                .failureReason(report.getFailureReason())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
