package myexpressionfriend_api.statistics.expression.dto;

import java.util.List;
import java.util.UUID;

public record ExpressionSummaryDto(
        UUID childId,
        List<EmotionStatDto> emotions,
        List<String> topImprovedEmotions,
        String encouragementMessage
) {
    public record EmotionStatDto(
            String emotion,
            double successRate,
            String successRateLevel,
            double fluencyIndex,
            String fluencyLevelForParent,
            int sessionCount,
            boolean dataReady,
            Double validSessionRate,
            Double avgSessionDurationSec,
            Double retryReductionRate,
            String retryBaselineStatus,
            Double trendSlope,
            String trendDirection,
            Double confidenceScore,
            String confidenceLevel,
            List<SessionTrendDto> sessionTrend
    ) {}

    public record SessionTrendDto(
            int sessionNumber,
            double finalAccuracy,
            boolean isSuccess
    ) {}
}
