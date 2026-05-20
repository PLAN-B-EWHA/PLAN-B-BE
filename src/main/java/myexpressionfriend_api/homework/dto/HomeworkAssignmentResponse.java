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
        HomeworkStatus status,
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
                homework.getStatus(),
                homework.getCreatedAt(),
                report
        );
    }
}
