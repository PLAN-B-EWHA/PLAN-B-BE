package myexpressionfriend_api.homework.dto;

import java.util.List;
import java.util.UUID;

public record HomeworkMissionSummaryResponse(
        UUID childId,
        int assignedCount,
        int pendingCount,
        int submittedCount,
        int reviewedCount,
        int canceledCount,
        int overduePendingCount,
        int dueSoonPendingCount,
        int doneCount,
        int partialCount,
        int notDoneCount,
        int spontaneousCount,
        double submissionRate,
        double completionRate,
        double successRate,
        double spontaneousRate,
        List<WeekSummary> weeks
) {
    public record WeekSummary(
            int week,
            String strategyFocus,
            String strategyFocusLabel,
            int assignedCount,
            int submittedCount,
            int doneCount,
            int partialCount,
            int notDoneCount,
            double submissionRate,
            double completionRate,
            double successRate,
            double spontaneousRate
    ) {}
}
