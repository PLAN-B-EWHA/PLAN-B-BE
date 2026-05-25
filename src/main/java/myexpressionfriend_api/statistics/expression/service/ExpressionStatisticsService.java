package myexpressionfriend_api.statistics.expression.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.config.ExpressionBaselineProperties;
import myexpressionfriend_api.common.config.StatisticsProperties;
import myexpressionfriend_api.game.domain.ExpressionSession;
import myexpressionfriend_api.game.domain.ExpressionTry;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.statistics.common.StatisticsCalculator;
import myexpressionfriend_api.statistics.expression.domain.ExpressionStatSummary;
import myexpressionfriend_api.statistics.expression.repository.ExpressionDurationAverageProjection;
import myexpressionfriend_api.statistics.expression.repository.ExpressionSessionAggregateProjection;
import myexpressionfriend_api.statistics.expression.repository.ExpressionStatSummaryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpressionStatisticsService {

    private final ExpressionStatSummaryRepository summaryRepository;
    private final ExpressionSessionRepository sessionRepository;
    private final ExpressionBaselineProperties baselineProperties;
    private final StatisticsProperties statisticsProperties;
    private final ChildRepository childRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void upsertForSession(UUID childId, ExpressionSession session) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            upsertForSessionInternal(childId, session);
            meterRegistry.counter("statistics.expression.upsert", "result", "success").increment();
        } catch (RuntimeException ex) {
            meterRegistry.counter("statistics.expression.upsert", "result", "failure").increment();
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("statistics.expression.upsert.duration"));
        }
    }

    private void upsertForSessionInternal(UUID childId, ExpressionSession session) {
        Child child = childRepository.findById(childId).orElseThrow();
        String emotion = session.getEmotionTarget();

        int minSec = statisticsProperties.getSessionDuration().getMinSec();
        int maxSec = statisticsProperties.getSessionDuration().getExpressionMaxSec();
        ExpressionSessionAggregateProjection aggregate = sessionRepository
                .aggregateByChildAndEmotion(childId, emotion, minSec, maxSec);

        int sessionCount = aggregate.getSessionCount();
        double successRate = sessionCount > 0 ? (double) aggregate.getSuccessCount() / sessionCount : 0.0;
        double avgRetry = aggregate.getAvgRetry();
        double fluencyIndex = calcFluencyIndex(session);
        double durationDecreaseRate = calcSessionDurationDecreaseRate(childId, emotion, minSec, maxSec);

        // 중간버전: 고급 통계(CI, 수렴속도) 비활성화
        Double[] ci = new Double[]{null, null};
        Double convergenceSpeed = null;

        double validSessionRate = sessionCount > 0 ? (double) aggregate.getValidSessionCount() / sessionCount : 0.0;
        Double avgDurationSec = aggregate.getAvgSessionDurationSec();

        ExpressionStatSummary summary = summaryRepository
                .findByChild_ChildIdAndEmotionTarget(childId, emotion)
                .orElseGet(() -> ExpressionStatSummary.builder()
                        .child(child)
                        .emotionTarget(emotion)
                        .build());

        RetryBaselineResult retryResult = calcRetryBaseline(childId, emotion, sessionCount);
        TrendConfidence trendConfidence = calcTrendConfidence(childId, emotion, sessionCount);

        summary.update(successRate, fluencyIndex, avgRetry, sessionCount, durationDecreaseRate,
                ci[0], ci[1], convergenceSpeed, validSessionRate, avgDurationSec,
                retryResult.reductionRate(), retryResult.baselineStatus(),
                trendConfidence.trendSlope(), trendConfidence.trendDirection(),
                trendConfidence.confidenceScore(), trendConfidence.confidenceLevel());
        summaryRepository.save(summary);
    }

    /** 1-2. 유창성 지수 (정확-속도 복합 점수) */
    public double calcFluencyIndex(ExpressionSession session) {
        List<ExpressionTry> tries = session.getTries();
        if (tries == null || tries.isEmpty()) return 0.0;

        int baseline = baselineProperties.getBaseline(session.getEmotionTarget());
        float firstTryAccuracy = tries.get(0).getAccuracyScore();
        int lastDuration = tries.get(tries.size() - 1).getDurationMs();
        int tryCount = tries.size();

        if (lastDuration <= 0 || firstTryAccuracy <= 0) return 0.0;

        double triesPenalty = 1.0 / Math.sqrt(tryCount);
        return firstTryAccuracy * ((double) baseline / lastDuration) * triesPenalty * 100.0;
    }

    /** 재시도 감소율 */
    public double calcDurationDecreaseRate(List<ExpressionTry> tries) {
        if (tries == null || tries.size() < 2) return 0.0;
        int first = tries.get(0).getDurationMs();
        int last = tries.get(tries.size() - 1).getDurationMs();
        return first > 0 ? (double) (first - last) / first : 0.0;
    }

    public String resolveFluencyLevelForParent(double fluencyIndex) {
        if (fluencyIndex >= 80) return "자연스럽게 표현해요";
        if (fluencyIndex >= 50) return "아직은 연습 중이에요";
        return "조금 더 연습이 필요해요";
    }

    /** 1-1. 감정별 성공률 */
    public String resolveSuccessRateLevel(double successRate) {
        double pct = successRate * 100;
        if (pct >= 80) return "안정적 숙달";
        if (pct >= 60) return "개선 중";
        return "집중 지도가 필요";
    }

    public boolean isDataReady(int sessionCount) {
        return sessionCount >= 3;
    }

    public RetryBaselineResult calcRetryBaseline(UUID childId, String emotion, int sessionCount) {
        if (sessionCount < 4) {
            return new RetryBaselineResult(null, "INSUFFICIENT");
        }

        List<ExpressionSession> baselineSessions = sessionRepository.findOldestByChildAndEmotion(
                childId, emotion, PageRequest.of(0, 4));
        List<ExpressionSession> recentSessions = sessionRepository.findRecentByChildAndEmotion(
                childId, emotion, PageRequest.of(0, 4));
        if (baselineSessions.size() < 4 || recentSessions.size() < 4) {
            return new RetryBaselineResult(null, "INSUFFICIENT");
        }

        double baselineAvgRetry = baselineSessions.stream()
                .mapToInt(s -> s.getTotalTries() != null ? s.getTotalTries() : 1)
                .average()
                .orElse(0.0);
        double recentAvgRetry = recentSessions.stream()
                .mapToInt(s -> s.getTotalTries() != null ? s.getTotalTries() : 1)
                .average()
                .orElse(0.0);

        if (baselineAvgRetry <= 0) {
            return new RetryBaselineResult(null, "INSUFFICIENT");
        }

        int firstSessionTries = baselineSessions.get(0).getTotalTries() != null ? baselineSessions.get(0).getTotalTries() : 1;
        if (recentAvgRetry > 0 && firstSessionTries >= recentAvgRetry * 2.0) {
            double rate = (baselineAvgRetry - recentAvgRetry) / baselineAvgRetry;
            return new RetryBaselineResult(rate, "ANOMALY");
        }

        double rate = (baselineAvgRetry - recentAvgRetry) / baselineAvgRetry;
        return new RetryBaselineResult(rate, "VALID");
    }

    public double calcSessionDurationDecreaseRate(UUID childId, String emotion, int minSec, int maxSec) {
        ExpressionDurationAverageProjection baseline = sessionRepository.avgOldestValidDurationByChildAndEmotion(
                childId, emotion, minSec, maxSec, 3);
        ExpressionDurationAverageProjection recent = sessionRepository.avgRecentValidDurationByChildAndEmotion(
                childId, emotion, minSec, maxSec, 3);

        Double baselineAvg = baseline.getAvgDurationSec();
        Double recentAvg = recent.getAvgDurationSec();
        if (baselineAvg == null || recentAvg == null || baselineAvg <= 0) {
            return 0.0;
        }
        return (baselineAvg - recentAvg) / baselineAvg;
    }

    public TrendConfidence calcTrendConfidence(UUID childId, String emotion, int sessionCount) {
        List<ExpressionSession> recentSessions = new ArrayList<>(sessionRepository.findRecentByChildAndEmotion(
                childId, emotion, PageRequest.of(0, 6)));
        Collections.reverse(recentSessions);

        List<Double> accuracyValues = recentSessions.stream()
                .map(ExpressionSession::getFinalAccuracy)
                .map(v -> v != null ? v.doubleValue() : 0.0)
                .toList();
        double slope = statisticsCalculator.trendSlope(accuracyValues);
        String direction = statisticsCalculator.trendDirection(slope);
        double confidenceScore = statisticsCalculator.confidenceScore(
                sessionCount,
                null,
                recentSessions.isEmpty() ? null : recentSessions.get(recentSessions.size() - 1).getStartedAt());

        return new TrendConfidence(slope, direction, confidenceScore,
                statisticsCalculator.confidenceLevel(confidenceScore));
    }

    public record RetryBaselineResult(Double reductionRate, String baselineStatus) {}

    public record TrendConfidence(
            Double trendSlope,
            String trendDirection,
            Double confidenceScore,
            String confidenceLevel
    ) {}
}
