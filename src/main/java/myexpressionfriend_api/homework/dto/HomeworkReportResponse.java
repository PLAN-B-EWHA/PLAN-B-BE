package myexpressionfriend_api.homework.dto;

import myexpressionfriend_api.homework.domain.CompletionStatus;
import myexpressionfriend_api.homework.domain.HomeworkReport;
import myexpressionfriend_api.homework.domain.InitiationType;
import myexpressionfriend_api.homework.domain.StrategyFocus;

import java.time.LocalDateTime;
import java.util.UUID;

public record HomeworkReportResponse(
        UUID reportId,
        UUID homeworkId,
        UUID reportedBy,
        UUID reviewedBy,
        CompletionStatus completed,
        InitiationType initiatedBy,
        StrategyFocus strategyApplied,
        String parentObservation,
        String peerResponseObserved,
        Boolean spontaneousFlag,
        LocalDateTime reportedAt,
        String therapistReviewComment,
        LocalDateTime reviewedAt
) {
    public static HomeworkReportResponse from(HomeworkReport report) {
        if (report == null) {
            return null;
        }
        return new HomeworkReportResponse(
                report.getReportId(),
                report.getHomework().getHomeworkId(),
                report.getReportedBy().getUserId(),
                report.getReviewedBy() == null ? null : report.getReviewedBy().getUserId(),
                report.getCompleted(),
                report.getInitiatedBy(),
                report.getStrategyApplied(),
                report.getParentObservation(),
                report.getPeerResponseObserved(),
                report.getSpontaneousFlag(),
                report.getReportedAt(),
                report.getTherapistReviewComment(),
                report.getReviewedAt()
        );
    }
}
