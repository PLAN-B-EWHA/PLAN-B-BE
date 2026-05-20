package myexpressionfriend_api.statistics.dialogue.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.game.domain.DialogueSession;
import myexpressionfriend_api.game.domain.DialogueTurn;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.DialogueTurnRepository;
import myexpressionfriend_api.statistics.dialogue.domain.DialogueStatSummary;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueStatSummaryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DialogueTurnRepository turnRepository;
    private final ChildRepository childRepository;

    @Transactional
    public void upsertForSession(UUID childId, DialogueSession session) {
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

        Double retryReductionRate = calcRetryReductionRate(childId, theme, avgScoreRate > 0 ? qualityDist[0] : null);

        DialogueStatSummary summary = existing != null ? existing
                : DialogueStatSummary.builder().child(child).theme(theme).build();

        summary.update(avgScoreRate, rapportIndex != null ? rapportIndex : 0.0,
                turnFatigue, qualityDist[0], qualityDist[1], qualityDist[2], (int) sessionCount,
                emaValue, alpha, consistencyStd,
                optionBiasDetected, biasedOptionOrder, retryReductionRate);
        summaryRepository.save(summary);
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
        double[] rates = recent.stream().mapToDouble(s -> s.getScoreRate() != null ? s.getScoreRate() : 0.0).toArray();
        double mean = (rates[0] + rates[1] + rates[2]) / 3.0;
        double variance = (Math.pow(rates[0] - mean, 2) + Math.pow(rates[1] - mean, 2) + Math.pow(rates[2] - mean, 2)) / 3.0;
        return Math.sqrt(variance);
    }

    public Double calcRetryReductionRate(UUID childId, PeersTheme theme, Double currentScore0Rate) {
        if (currentScore0Rate == null) return null;

        List<DialogueSession> baseline = sessionRepository.findOldestByChildAndTheme(
                childId, theme, PageRequest.of(0, 4));
        if (baseline.size() < 4) return null;

        double baselineScore0Rate = baseline.stream()
                .mapToDouble(s -> {
                    List<DialogueTurn> turns = s.getTurns();
                    if (turns == null || turns.isEmpty()) return 0.0;
                    long zeroCount = turns.stream().filter(t -> t.getSelectedScore() == 0).count();
                    return (double) zeroCount / turns.size();
                })
                .average()
                .orElse(0.0);

        if (baselineScore0Rate <= 0) return null;
        return (baselineScore0Rate - currentScore0Rate) / baselineScore0Rate;
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
        if (emaValue == null) return "Collecting data";
        if (emaValue >= 0.80 && consistencyStd != null && consistencyStd <= 0.15) return "Mastered";
        if (emaValue >= 0.80) return "High but unstable";
        if (emaValue >= 0.50) return "In progress";
        return "Needs focused support";
    }

    public String resolveMasteryJudgmentForParent(Double emaValue, Double consistencyStd) {
        if (emaValue == null) return "Collecting records";
        if (emaValue >= 0.80 && consistencyStd != null && consistencyStd <= 0.15) return "Doing very well";
        if (emaValue >= 0.80) return "Doing well with practice";
        if (emaValue >= 0.50) return "Still practicing";
        return "Needs more practice";
    }

    public String resolveRapportLevel(double rapportIndex) {
        if (rapportIndex >= 0.75) return "Strong positive response";
        if (rapportIndex >= 0.50) return "Improving";
        return "Needs focused support";
    }
}
