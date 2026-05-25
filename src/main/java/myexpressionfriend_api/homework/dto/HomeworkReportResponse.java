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
        String completedLabel,
        InitiationType initiatedBy,
        String initiatedByLabel,
        StrategyFocus strategyApplied,
        String strategyAppliedLabel,
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
                completedLabel(report.getCompleted()),
                report.getInitiatedBy(),
                initiatedByLabel(report.getInitiatedBy()),
                report.getStrategyApplied(),
                strategyAppliedLabel(report.getStrategyApplied()),
                report.getParentObservation(),
                report.getPeerResponseObserved(),
                report.getSpontaneousFlag(),
                report.getReportedAt(),
                report.getTherapistReviewComment(),
                report.getReviewedAt()
        );
    }

    private static String completedLabel(CompletionStatus completed) {
        if (completed == null) return null;
        return switch (completed) {
            case DONE -> "완료";
            case PARTIAL -> "부분 수행";
            case NOT_DONE -> "수행하지 못함";
        };
    }

    private static String initiatedByLabel(InitiationType initiatedBy) {
        if (initiatedBy == null) return null;
        return switch (initiatedBy) {
            case SELF -> "아이 스스로 시작";
            case HINT -> "힌트 후 시작";
            case PROMPTED -> "직접 안내 후 시작";
        };
    }

    private static String strategyAppliedLabel(StrategyFocus strategyApplied) {
        if (strategyApplied == null) return null;
        return switch (strategyApplied) {
            case INFORMATION_EXCHANGE -> "정보 교환하기";
            case CONVERSATION_MAINTENANCE -> "대화 유지하기";
            case FINDING_COMMON_GROUND -> "공통점 찾기";
            case CONVERSATION_INITIATION -> "대화 시작하기";
            case CONVERSATION_EXIT -> "대화 마무리하기";
            case DIGITAL_COMMUNICATION -> "전자 의사소통";
            case FRIEND_SELECTION -> "친구 선택하기";
            case HUMOR_USE -> "유머 사용하기";
            case GOOD_SPORTSMANSHIP -> "좋은 스포츠맨십";
            case PLAYING_TOGETHER -> "함께 놀기";
            case CONFLICT_RESOLUTION -> "갈등 해결하기";
            case HANDLING_TEASING -> "놀림에 대처하기";
            case HANDLING_EXCLUSION -> "소외에 대처하기";
            case HANDLING_CYBERBULLYING -> "사이버 괴롭힘 대처하기";
            case HANDLING_RUMORS -> "소문과 험담 대처하기";
            case REPUTATION_MANAGEMENT -> "평판 관리하기";
        };
    }
}
