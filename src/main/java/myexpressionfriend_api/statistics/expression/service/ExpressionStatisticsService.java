package myexpressionfriend_api.statistics.expression.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.config.ExpressionBaselineProperties;
import myexpressionfriend_api.common.config.StatisticsProperties;
import myexpressionfriend_api.game.domain.ExpressionSession;
import myexpressionfriend_api.game.domain.ExpressionTry;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.statistics.expression.domain.ExpressionStatSummary;
import myexpressionfriend_api.statistics.expression.repository.ExpressionStatSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    @Transactional
    public void upsertForSession(UUID childId, ExpressionSession session) {
        Child child = childRepository.findById(childId).orElseThrow();
        String emotion = session.getEmotionTarget();

        List<ExpressionSession> allSessions = sessionRepository.findAllByChildIdAndEmotionOrderByStartedAt(childId, emotion);
        int sessionCount = allSessions.size();
        long successCount = allSessions.stream().filter(s -> Boolean.TRUE.equals(s.getIsSuccess())).count();

        double successRate = sessionCount > 0 ? (double) successCount / sessionCount : 0.0;
        double avgRetry = allSessions.stream().mapToInt(s -> s.getTotalTries() != null ? s.getTotalTries() : 1).average().orElse(0.0);
        double fluencyIndex = calcFluencyIndex(session);
        double durationDecreaseRate = calcDurationDecreaseRate(session.getTries());

        // 중간버전: 고급 통계(CI, 수렴속도) 비활성화
        Double[] ci = new Double[]{null, null};
        Double convergenceSpeed = null;

        int minSec = statisticsProperties.getSessionDuration().getMinSec();
        int maxSec = statisticsProperties.getSessionDuration().getExpressionMaxSec();
        List<ExpressionSession> validSessions = allSessions.stream()
                .filter(s -> isValidDuration(s, minSec, maxSec))
                .toList();
        double validSessionRate = sessionCount > 0 ? (double) validSessions.size() / sessionCount : 0.0;
        Double avgDurationSec = validSessions.isEmpty() ? null
                : validSessions.stream()
                .mapToLong(s -> Duration.between(s.getStartedAt(), s.getEndedAt()).getSeconds())
                .average()
                .stream().boxed().findFirst().orElse(null);

        ExpressionStatSummary summary = summaryRepository
                .findByChild_ChildIdAndEmotionTarget(childId, emotion)
                .orElseGet(() -> ExpressionStatSummary.builder()
                        .child(child)
                        .emotionTarget(emotion)
                        .build());

        RetryBaselineResult retryResult = calcRetryBaseline(allSessions, avgRetry);

        summary.update(successRate, fluencyIndex, avgRetry, sessionCount, durationDecreaseRate,
                ci[0], ci[1], convergenceSpeed, validSessionRate, avgDurationSec,
                retryResult.reductionRate(), retryResult.baselineStatus());
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

    private boolean isValidDuration(ExpressionSession s, int minSec, int maxSec) {
        if (s.getStartedAt() == null || s.getEndedAt() == null) return false;
        long sec = Duration.between(s.getStartedAt(), s.getEndedAt()).getSeconds();
        return sec >= minSec && sec <= maxSec;
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

    public RetryBaselineResult calcRetryBaseline(List<ExpressionSession> allSessions, double currentAvgRetry) {
        if (allSessions.size() < 4) {
            return new RetryBaselineResult(null, "INSUFFICIENT");
        }

        List<ExpressionSession> baselineSessions = allSessions.subList(0, 4);
        double baselineAvgRetry = baselineSessions.stream()
                .mapToInt(s -> s.getTotalTries() != null ? s.getTotalTries() : 1)
                .average()
                .orElse(0.0);

        if (baselineAvgRetry <= 0) {
            return new RetryBaselineResult(null, "INSUFFICIENT");
        }

        int firstSessionTries = allSessions.get(0).getTotalTries() != null ? allSessions.get(0).getTotalTries() : 1;
        if (currentAvgRetry > 0 && firstSessionTries >= currentAvgRetry * 2.0) {
            double rate = (baselineAvgRetry - currentAvgRetry) / baselineAvgRetry;
            return new RetryBaselineResult(rate, "ANOMALY");
        }

        double rate = (baselineAvgRetry - currentAvgRetry) / baselineAvgRetry;
        return new RetryBaselineResult(rate, "VALID");
    }

    public record RetryBaselineResult(Double reductionRate, String baselineStatus) {}
}
