package myexpressionfriend_api.statistics.dashboard.dto;

import java.util.List;
import java.util.UUID;

public record DialogueProgressDto(
        UUID childId,
        List<ThemeProgressItem> themes
) {
    public record ThemeProgressItem(
            int weekNumber,
            String theme,
            String status,
            String statusLabelParent,
            String statusLabelTherapist,
            int sessionCount,
            Double emaValue,
            Double consistencyStd,
            OfflineMissionProgressDto offlineMission,
            boolean offlineGeneralized
    ) {}

    public record OfflineMissionProgressDto(
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
