package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.game.domain.DialogueSession;
import myexpressionfriend_api.game.domain.ExpressionSession;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.statistics.dashboard.dto.DialogueProgressDto;
import myexpressionfriend_api.statistics.dialogue.domain.DialogueStatSummary;
import myexpressionfriend_api.statistics.dialogue.dto.DialogueSummaryDto;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueStatSummaryRepository;
import myexpressionfriend_api.statistics.dialogue.service.DialogueStatisticsService;
import myexpressionfriend_api.statistics.dialogue.util.ScenarioWeekParser;
import myexpressionfriend_api.statistics.expression.domain.ExpressionStatSummary;
import myexpressionfriend_api.statistics.expression.dto.ExpressionSummaryDto;
import myexpressionfriend_api.statistics.expression.repository.ExpressionStatSummaryRepository;
import myexpressionfriend_api.statistics.expression.service.ExpressionStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardSummaryAssembler {

    private final ExpressionStatSummaryRepository expressionStatSummaryRepository;
    private final DialogueStatSummaryRepository dialogueStatSummaryRepository;
    private final ExpressionStatisticsService expressionStatisticsService;
    private final DialogueStatisticsService dialogueStatisticsService;
    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
    private final OfflineMissionStatisticsService offlineMissionStatisticsService;
    @Transactional(readOnly = true)
    public ExpressionSummaryDto buildExpressionSummary(UUID childId, boolean includeEncouragementMessage) {
        List<ExpressionStatSummary> summaries = expressionStatSummaryRepository.findByChild_ChildId(childId);
        List<ExpressionSummaryDto.EmotionStatDto> emotionStats = summaries.stream()
                .map(s -> {
                    List<ExpressionSummaryDto.SessionTrendDto> trend = expressionSessionRepository
                            .findSessionTrendByChildAndEmotion(childId, s.getEmotionTarget())
                            .stream()
                            .map(p -> new ExpressionSummaryDto.SessionTrendDto(
                                    p.getSessionNumber(), p.getFinalAccuracy(), p.getIsSuccess()))
                            .toList();

                    return new ExpressionSummaryDto.EmotionStatDto(
                            s.getEmotionTarget(),
                            s.getSuccessRate(),
                            expressionStatisticsService.resolveSuccessRateLevel(s.getSuccessRate()),
                            s.getFluencyIndex(),
                            expressionStatisticsService.resolveFluencyLevelForParent(s.getFluencyIndex()),
                            s.getSessionCount(),
                            expressionStatisticsService.isDataReady(s.getSessionCount()),
                            s.getValidSessionRate(),
                            s.getAvgSessionDurationSec(),
                            s.getRetryReductionRate(),
                            s.getRetryBaselineStatus(),
                            s.getTrendSlope(),
                            s.getTrendDirection(),
                            s.getConfidenceScore(),
                            s.getConfidenceLevel(),
                            trend
                    );
                })
                .toList();

        List<String> topImproved = summaries.stream()
                .filter(s -> expressionStatisticsService.isDataReady(s.getSessionCount()))
                .sorted(Comparator.comparingDouble(ExpressionStatSummary::getDurationDecreaseRate).reversed())
                .limit(3)
                .map(ExpressionStatSummary::getEmotionTarget)
                .toList();

        String message = includeEncouragementMessage ? buildEncouragementMessage(childId, emotionStats) : null;
        return new ExpressionSummaryDto(childId, emotionStats, topImproved, message);
    }

    @Transactional(readOnly = true)
    public List<DialogueSummaryDto> buildDialogueSummaries(UUID childId) {
        return dialogueStatSummaryRepository.findByChild_ChildId(childId).stream()
                .map(summary -> {
                    List<DialogueSummaryDto.WeeklyTrendDto> weeklyTrend = dialogueSessionRepository
                            .findWeeklyProgressByChildAndTheme(childId, summary.getTheme().getDisplayName())
                            .stream()
                            .map(p -> new DialogueSummaryDto.WeeklyTrendDto(p.getWeekNumber(), p.getScoreRate()))
                            .toList();

                    return new DialogueSummaryDto(
                            childId,
                            summary.getTheme().getDisplayName(),
                            summary.getScoreRate(),
                            new DialogueSummaryDto.QualityDistributionDto(
                                    summary.getScore0Rate(), summary.getScore1Rate(), summary.getScore2Rate()),
                            weeklyTrend,
                            expressionStatisticsService.isDataReady(summary.getSessionCount()),
                            summary.getSessionCount(),
                            resolveGraphPhase(weeklyTrend.size()),
                            summary.getEmaValue(),
                            summary.getEmaAlpha(),
                            summary.getConsistencyStd(),
                            dialogueStatisticsService.resolveMasteryJudgmentForParent(
                                    summary.getEmaValue(), summary.getConsistencyStd()),
                            summary.getRetryReductionRate(),
                            summary.getTrendSlope(),
                            summary.getTrendDirection(),
                            summary.getConfidenceScore(),
                            summary.getConfidenceLevel()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DialogueProgressDto buildDialogueProgress(UUID childId, boolean therapistView) {
        Map<PeersTheme, DialogueStatSummary> summaryMap = dialogueStatSummaryRepository
                .findByChild_ChildId(childId).stream()
                .collect(Collectors.toMap(DialogueStatSummary::getTheme, s -> s));

        Map<Integer, Long> playedWeekSessionCount = dialogueSessionRepository.findScenarioIdsByChildId(childId).stream()
                .map(ScenarioWeekParser::parseWeek)
                .filter(OptionalInt::isPresent)
                .mapToInt(OptionalInt::getAsInt)
                .boxed()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        Map<Integer, DialogueProgressDto.OfflineMissionProgressDto> offlineProgressByWeek =
                offlineMissionStatisticsService.getProgressByWeek(childId);

        List<DialogueProgressDto.ThemeProgressItem> items = Arrays.stream(PeersTheme.values())
                .sorted(Comparator.comparingInt(PeersTheme::getWeekNumber))
                .map(theme -> buildProgressItem(theme, summaryMap.get(theme),
                        playedWeekSessionCount.getOrDefault(theme.getWeekNumber(), 0L).intValue(),
                        offlineProgressByWeek.get(theme.getWeekNumber()),
                        therapistView))
                .toList();

        return new DialogueProgressDto(childId, items);
    }

    private DialogueProgressDto.ThemeProgressItem buildProgressItem(
            PeersTheme theme,
            DialogueStatSummary summary,
            int weekSessionCount,
            DialogueProgressDto.OfflineMissionProgressDto offlineProgress,
            boolean therapistView
    ) {
        if (summary == null) {
            String status = weekSessionCount > 0 ? "IN_PROGRESS" : "NOT_STARTED";
            String label = weekSessionCount > 0 ? "진행 중" : "미시작";
            return new DialogueProgressDto.ThemeProgressItem(
                    theme.getWeekNumber(), theme.getDisplayName(),
                    status, label, therapistView ? label : null,
                    weekSessionCount, null, null, offlineProgress);
        }

        boolean mastered = summary.getEmaValue() != null
                && summary.getEmaValue() >= 0.65
                && summary.getSessionCount() >= 3;
        String parentLabel = mastered ? "완료" : "진행 중";
        String therapistLabel = null;
        if (therapistView) {
            therapistLabel = summary.getEmaValue() == null
                    ? "데이터 부족"
                    : String.format("%s (EMA %.2f)", mastered ? "도달" : "미숙달", summary.getEmaValue());
        }

        return new DialogueProgressDto.ThemeProgressItem(
                theme.getWeekNumber(),
                theme.getDisplayName(),
                mastered ? "COMPLETED" : "IN_PROGRESS",
                parentLabel,
                therapistLabel,
                summary.getSessionCount(),
                summary.getEmaValue(),
                summary.getConsistencyStd(),
                offlineProgress
        );
    }

    private String resolveGraphPhase(int weekCount) {
        if (weekCount >= 16) return "COMPLETE";
        if (weekCount >= 9) return "MID";
        return "EARLY";
    }

    private String buildEncouragementMessage(UUID childId, List<ExpressionSummaryDto.EmotionStatDto> emotionStats) {
        long readyCount = emotionStats.stream().filter(ExpressionSummaryDto.EmotionStatDto::dataReady).count();
        if (readyCount == 0) {
            return "기록이 쌓이는 중이에요. 조금만 더 참여하면 변화가 보여요.";
        }

        ZoneId zone = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zone);
        Instant thisWeekStart = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(zone).toInstant();
        Instant lastWeekStart = thisWeekStart.minus(Duration.ofDays(7));

        List<ExpressionSession> thisWeek = expressionSessionRepository.findSessionsBetween(childId, thisWeekStart, now.toInstant());
        List<ExpressionSession> lastWeek = expressionSessionRepository.findSessionsBetween(childId, lastWeekStart, thisWeekStart);

        if (!lastWeek.isEmpty() && !thisWeek.isEmpty()) {
            double thisAvg = thisWeek.stream().mapToInt(s -> Boolean.TRUE.equals(s.getIsSuccess()) ? 1 : 0).average().orElse(0);
            double lastAvg = lastWeek.stream().mapToInt(s -> Boolean.TRUE.equals(s.getIsSuccess()) ? 1 : 0).average().orElse(0);
            int diffPct = (int) Math.round((thisAvg - lastAvg) * 100);
            if (diffPct > 0) {
                return String.format("지난주보다 성공률이 %d%%p 올랐어요.", diffPct);
            }
            if (diffPct < 0) {
                return "이번 주도 꾸준히 참여하고 있어요. 충분히 해내고 있어요.";
            }
        }

        double avgSuccessRate = emotionStats.stream()
                .filter(ExpressionSummaryDto.EmotionStatDto::dataReady)
                .mapToDouble(ExpressionSummaryDto.EmotionStatDto::successRate)
                .average()
                .orElse(0.0);

        if (avgSuccessRate >= 0.8) return "표정 표현이 많이 자연스러워졌어요.";
        if (avgSuccessRate >= 0.5) return "꾸준히 연습 중이에요. 점점 좋아지고 있어요.";
        return "매일 조금씩 좋아지고 있어요. 계속 연습해요.";
    }
}
