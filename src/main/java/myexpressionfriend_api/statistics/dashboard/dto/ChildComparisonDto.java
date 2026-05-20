package myexpressionfriend_api.statistics.dashboard.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 4-1 담당 아동 비교 뷰
 * 치료사가 여러 아동의 동일 지표를 한 화면에서 비교
 */
public record ChildComparisonDto(
        List<ChildStat> children
) {
    public record ChildStat(
            UUID childId,
            String childName,
            // 표정: 감정별 성공률
            Map<String, Double> expressionSuccessRates,   // emotion → successRate
            // 표정: 주의 감정 (성공률 60% 미만)
            List<String> attentionEmotions,
            // 대화: 테마별 EMA
            Map<String, Double> dialogueEmaValues,        // theme → emaValue
            // 수렴 속도 (최근 세션 기준)
            Map<String, Double> convergenceSpeeds,        // emotion → convergenceSpeed
            int totalSessionCount,
            boolean hasEnoughData                         // 최소 3세션 보유 여부
    ) {}
}
