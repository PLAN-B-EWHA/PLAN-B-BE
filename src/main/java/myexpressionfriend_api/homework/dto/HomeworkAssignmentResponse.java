package myexpressionfriend_api.homework.dto;

import myexpressionfriend_api.homework.domain.HomeworkAssignment;
import myexpressionfriend_api.homework.domain.HomeworkStatus;
import myexpressionfriend_api.homework.domain.StrategyFocus;
import myexpressionfriend_api.homework.domain.StrategyTipSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record HomeworkAssignmentResponse(
        UUID homeworkId,
        UUID childId,
        UUID weeklyProgressId,
        Integer week,
        StrategyFocus strategyFocus,
        String instruction,
        String strategyTip,
        StrategyTipSource strategyTipSource,
        LocalDate dueDate,
        boolean overdue,
        boolean dueSoon,
        HomeworkStatus status,
        String statusLabel,
        String strategyFocusLabel,
        LocalDateTime createdAt,
        HomeworkReportResponse report
) {
    public static HomeworkAssignmentResponse from(HomeworkAssignment homework, HomeworkReportResponse report) {
        return new HomeworkAssignmentResponse(
                homework.getHomeworkId(),
                homework.getChild().getChildId(),
                homework.getWeeklyProgressId(),
                homework.getWeek(),
                homework.getStrategyFocus(),
                homework.getInstruction(),
                homework.getStrategyTip(),
                homework.getStrategyTipSource(),
                homework.getDueDate(),
                isOverdue(homework),
                isDueSoon(homework),
                homework.getStatus(),
                statusLabel(homework.getStatus()),
                strategyFocusLabel(homework.getStrategyFocus()),
                homework.getCreatedAt(),
                report
        );
    }

    private static boolean isOverdue(HomeworkAssignment homework) {
        return homework.getStatus() == HomeworkStatus.PENDING
                && homework.getDueDate() != null
                && homework.getDueDate().isBefore(LocalDate.now());
    }

    private static boolean isDueSoon(HomeworkAssignment homework) {
        return homework.getStatus() == HomeworkStatus.PENDING
                && homework.getDueDate() != null
                && !homework.getDueDate().isBefore(LocalDate.now())
                && !homework.getDueDate().isAfter(LocalDate.now().plusDays(2));
    }

    private static String statusLabel(HomeworkStatus status) {
        if (status == null) return null;
        return switch (status) {
            case PENDING -> "진행 전";
            case SUBMITTED -> "검토 대기";
            case REVIEWED -> "검토 완료";
            case CANCELED -> "취소됨";
            case EXPIRED -> "기한 만료";
        };
    }

    private static String strategyFocusLabel(StrategyFocus strategyFocus) {
        if (strategyFocus == null) return null;
        return switch (strategyFocus) {
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
