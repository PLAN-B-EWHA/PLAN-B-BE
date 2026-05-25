package myexpressionfriend_api.statistics.dialogue.dto;

import java.util.List;
import java.util.UUID;

public record DialogueSummaryDto(
        UUID childId,
        String theme,
        double strategyMasteryIndex,
        QualityDistributionDto qualityDistribution,
        List<WeeklyTrendDto> weeklyTrend,
        boolean dataReady,
        int currentSessionCount,
        String graphPhase,
        Double emaValue,
        Double emaAlpha,
        Double consistencyStd,
        String masteryJudgmentForParent,
        Double retryReductionRate,
        Double trendSlope,
        String trendDirection,
        Double confidenceScore,
        String confidenceLevel
) {
    public record QualityDistributionDto(double score0Rate, double score1Rate, double score2Rate) {}

    public record WeeklyTrendDto(int weekNumber, double scoreRate) {}
}
