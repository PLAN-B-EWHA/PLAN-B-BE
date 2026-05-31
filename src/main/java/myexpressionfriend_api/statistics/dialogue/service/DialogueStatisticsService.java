package myexpressionfriend_api.statistics.dialogue.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.game.domain.DialogueSession;
import myexpressionfriend_api.game.domain.DialogueTurn;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.statistics.common.StatisticsCalculator;
import myexpressionfriend_api.statistics.dialogue.domain.DialogueStatSummary;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueScore0RateProjection;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueStatSummaryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DialogueStatisticsService {

    private final DialogueStatSummaryRepository summaryRepository;
    private final DialogueSessionRepository sessionRepository;
    private final ChildRepository childRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void upsertForSession(UUID childId, DialogueSession session) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            upsertForSessionInternal(childId, session);
            meterRegistry.counter("statistics.dialogue.upsert", "result", "success").increment();
        } catch (RuntimeException ex) {
            meterRegistry.counter("statistics.dialogue.upsert", "result", "failure").increment();
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("statistics.dialogue.upsert.duration"));
        }
    }

    private void upsertForSessionInternal(UUID childId, DialogueSession session) {
        Child child = childRepository.findById(childId).orElseThrow();
        PeersTheme theme = session.getTheme();

        long sessionCount = sessionRepository.countByChildAndTheme(childId, theme);
        double avgScoreRate = sessionRepository.avgScoreRateByChildAndTheme(childId, theme);
        Double rapportIndex = sessionRepository.calcRapportIndexByChildAndTheme(childId, theme.getDisplayName());
        Double turnFatigue = null;
        double[] qualityDist = calcQualityDistribution(session.getTurns());

        DialogueStatSummary existing = summaryRepository
                .findByChild_ChildIdAndTheme(childId, theme)
                .orElse(null);

        double alpha = determineAlpha(childId, theme, (int) sessionCount, session.getStartedAt());
        Double emaValue = calcEma(childId, theme, session.getScoreRate(), (int) sessionCount,
                existing != null ? existing.getEmaValue() : null, alpha, session.getStartedAt());
        Double consistencyStd = calcConsistencyStd(childId, theme);

        Boolean optionBiasDetected = null;
        Integer biasedOptionOrder = null;

        Double retryReductionRate = calcRetryReductionRate(childId, theme, (int) sessionCount);
        TrendConfidence trendConfidence = calcTrendConfidence(childId, theme, (int) sessionCount, consistencyStd);

        DialogueStatSummary summary = existing != null ? existing
                : DialogueStatSummary.builder().child(child).theme(theme).build();

        summary.update(avgScoreRate, rapportIndex != null ? rapportIndex : 0.0,
                turnFatigue, qualityDist[0], qualityDist[1], qualityDist[2], (int) sessionCount,
                emaValue, alpha, consistencyStd,
                optionBiasDetected, biasedOptionOrder, retryReductionRate,
                trendConfidence.trendSlope(), trendConfidence.trendDirection(),
                trendConfidence.confidenceScore(), trendConfidence.confidenceLevel());
        summaryRepository.save(summary);
    }

    @Transactional
    public void recordOfflineOutcome(UUID childId, PeersTheme theme, boolean spontaneous) {
        // 비관적 락으로 동시 업데이트 경쟁을 방지한다.
        DialogueStatSummary summary = summaryRepository.findByChild_ChildIdAndThemeForUpdate(childId, theme)
                .orElse(null);
        if (summary == null) {
            return;
        }
        int newCount = zeroIfNull(summary.getOfflineReviewedCount()) + 1;
        int newSpontaneousCount = zeroIfNull(summary.getOfflineSpontaneousCount()) + (spontaneous ? 1 : 0);
        summary.updateOfflineOutcome(newCount, newSpontaneousCount, (double) newSpontaneousCount / newCount);
        summaryRepository.save(summary);
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    @Transactional
    public int rebuildForChild(UUID childId) {
        summaryRepository.deleteByChild_ChildId(childId);
        List<DialogueSession> sessions = sessionRepository.findByChild_ChildIdOrderByStartedAtAsc(childId);
        for (DialogueSession session : sessions) {
            upsertForSession(childId, session);
        }
        return sessions.size();
    }

    public double determineAlpha(UUID childId, PeersTheme theme, int totalSessionCount, Instant now) {
        if (totalSessionCount <= 3) return 0.0;

        Instant firstPlayedAt = sessionRepository.findFirstStartedAtByChildAndTheme(childId, theme)
                .orElse(now);
        boolean withinFirst4Weeks = Duration.between(firstPlayedAt, now).toDays() < 28;
        if (withinFirst4Weeks) {
            return totalSessionCount >= 9 ? 0.4 : 0.3;
        }

        Instant since = now.minus(28, ChronoUnit.DAYS);
        long recentCount = sessionRepository.countByChildAndThemeSince(childId, theme, since);
        return recentCount >= 12 ? 0.4 : 0.3;
    }

    public Double calcEma(UUID childId, PeersTheme theme, float currentScoreRate,
                          int sessionCount, Double prevEma, double alpha, Instant currentStartedAt) {
        if (sessionCount <= 3) {
            return sessionRepository.avgScoreRateByChildAndTheme(childId, theme);
        }
        if (prevEma == null) {
            return (double) currentScoreRate;
        }

        List<DialogueSession> recentSessions = sessionRepository.findRecentByChildAndTheme(
                childId, theme, PageRequest.of(0, 2));

        if (recentSessions.size() >= 2) {
            Instant previousStartedAt = recentSessions.get(1).getStartedAt();
            long absentDays = Duration.between(previousStartedAt, currentStartedAt).toDays();

            if (absentDays >= 28) {
                return (double) currentScoreRate;
            }
            if (absentDays >= 14) {
                double decayedEma = prevEma * 0.85;
                return alpha * currentScoreRate + (1 - alpha) * decayedEma;
            }
        }

        return alpha * currentScoreRate + (1 - alpha) * prevEma;
    }

    public Double calcConsistencyStd(UUID childId, PeersTheme theme) {
        List<DialogueSession> recent = sessionRepository.findRecentByChildAndTheme(
                childId, theme, PageRequest.of(0, 3));
        if (recent.size() < 3) return null;
        List<Double> rates = recent.stream()
                .map(s -> s.getScoreRate() != null ? s.getScoreRate().doubleValue() : 0.0)
                .toList();
        return statisticsCalculator.stddev(rates);
    }

    public Double calcRetryReductionRate(UUID childId, PeersTheme theme, int sessionCount) {
        if (sessionCount < 4) return null;

        DialogueScore0RateProjection baseline = sessionRepository.avgOldestScore0RateByChildAndTheme(
                childId, theme.getDisplayName(), 4);
        DialogueScore0RateProjection recent = sessionRepository.avgRecentScore0RateByChildAndTheme(
                childId, theme.getDisplayName(), 4);
        double baselineScore0Rate = baseline.getScore0Rate() != null ? baseline.getScore0Rate() : 0.0;
        double recentScore0Rate = recent.getScore0Rate() != null ? recent.getScore0Rate() : 0.0;

        if (baselineScore0Rate <= 0) return null;
        return (baselineScore0Rate - recentScore0Rate) / baselineScore0Rate;
    }

    public TrendConfidence calcTrendConfidence(
            UUID childId,
            PeersTheme theme,
            int sessionCount,
            Double consistencyStd
    ) {
        List<DialogueSession> recentSessions = new ArrayList<>(sessionRepository.findRecentByChildAndTheme(
                childId, theme, PageRequest.of(0, 6)));
        Collections.reverse(recentSessions);

        List<Double> scoreValues = recentSessions.stream()
                .map(DialogueSession::getScoreRate)
                .map(v -> v != null ? v.doubleValue() : 0.0)
                .toList();
        double slope = statisticsCalculator.trendSlope(scoreValues);
        String direction = statisticsCalculator.trendDirection(slope);
        double confidenceScore = statisticsCalculator.confidenceScore(
                sessionCount,
                consistencyStd,
                recentSessions.isEmpty() ? null : recentSessions.get(recentSessions.size() - 1).getStartedAt());

        return new TrendConfidence(slope, direction, confidenceScore,
                statisticsCalculator.confidenceLevel(confidenceScore));
    }

    public double[] calcQualityDistribution(List<DialogueTurn> turns) {
        if (turns == null || turns.isEmpty()) return new double[]{0, 0, 0};
        long total = turns.size();
        long s0 = turns.stream().filter(t -> t.getSelectedScore() == 0).count();
        long s1 = turns.stream().filter(t -> t.getSelectedScore() == 1).count();
        long s2 = turns.stream().filter(t -> t.getSelectedScore() == 2).count();
        return new double[]{(double) s0 / total, (double) s1 / total, (double) s2 / total};
    }

    public String resolveMasteryJudgment(Double emaValue, Double consistencyStd) {
        if (emaValue == null) return "데이터 수집 중";
        if (emaValue >= 0.80 && consistencyStd != null && consistencyStd <= 0.15) return "숙달";
        if (emaValue >= 0.80) return "높지만 불안정";
        if (emaValue >= 0.50) return "진행 중";
        return "집중 지도 필요";
    }

    public String resolveMasteryJudgmentForParent(Double emaValue, Double consistencyStd) {
        if (emaValue == null) return "기록 수집 중";
        if (emaValue >= 0.80 && consistencyStd != null && consistencyStd <= 0.15) return "아주 잘 하고 있어요";
        if (emaValue >= 0.80) return "잘 연습하고 있어요";
        if (emaValue >= 0.50) return "아직 연습 중이에요";
        return "연습이 더 필요해요";
    }

    public String resolveRapportLevel(double rapportIndex) {
        if (rapportIndex >= 0.75) return "긍정적 반응이 강해요";
        if (rapportIndex >= 0.50) return "개선되고 있어요";
        return "집중 지도가 필요해요";
    }

    public record TrendConfidence(
            Double trendSlope,
            String trendDirection,
            Double confidenceScore,
            String confidenceLevel
    ) {}
}
