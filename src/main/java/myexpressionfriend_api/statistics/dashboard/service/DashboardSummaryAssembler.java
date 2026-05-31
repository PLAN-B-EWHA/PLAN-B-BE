package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.config.StatisticsProperties;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.game.domain.DialogueSession;
import myexpressionfriend_api.game.domain.DialogueTurn;
import myexpressionfriend_api.game.domain.ExpressionSession;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.homework.domain.HomeworkReport;
import myexpressionfriend_api.statistics.dashboard.dto.DialogueProgressDto;
import myexpressionfriend_api.statistics.dashboard.dto.WeeklyHighlightDto;
import myexpressionfriend_api.statistics.dashboard.dto.WeeklyParticipationDto;
import myexpressionfriend_api.statistics.dialogue.domain.DialogueStatSummary;
import myexpressionfriend_api.statistics.dialogue.dto.DialogueSummaryDto;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueAllThemeProgressProjection;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueBestScoreProjection;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueStatSummaryRepository;
import myexpressionfriend_api.statistics.dialogue.service.DialogueStatisticsService;
import myexpressionfriend_api.statistics.dialogue.util.ScenarioWeekParser;
import myexpressionfriend_api.statistics.expression.domain.ExpressionStatSummary;
import myexpressionfriend_api.statistics.expression.dto.ExpressionSummaryDto;
import myexpressionfriend_api.statistics.expression.repository.ExpressionAllEmotionTrendProjection;
import myexpressionfriend_api.statistics.expression.repository.ExpressionBestAccuracyProjection;
import myexpressionfriend_api.statistics.expression.repository.ExpressionStatSummaryRepository;
import myexpressionfriend_api.statistics.expression.service.ExpressionStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
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
    private final StatisticsProperties statisticsProperties;

    @Transactional(readOnly = true)
    public ExpressionSummaryDto buildExpressionSummary(UUID childId, boolean includeEncouragementMessage) {
        List<ExpressionStatSummary> summaries = expressionStatSummaryRepository.findByChild_ChildId(childId);

        // 감정별 트렌드 배치 조회 → Map으로 변환 (N+1 방지)
        Map<String, List<ExpressionSummaryDto.SessionTrendDto>> trendByEmotion =
                expressionSessionRepository.findAllSessionTrendsByChild(childId).stream()
                        .collect(Collectors.groupingBy(
                                ExpressionAllEmotionTrendProjection::getEmotionTarget,
                                Collectors.mapping(
                                        p -> new ExpressionSummaryDto.SessionTrendDto(
                                                p.getSessionNumber(), p.getFinalAccuracy(), p.getIsSuccess()),
                                        Collectors.toList())));

        List<ExpressionSummaryDto.EmotionStatDto> emotionStats = summaries.stream()
                .map(s -> new ExpressionSummaryDto.EmotionStatDto(
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
                        trendByEmotion.getOrDefault(s.getEmotionTarget(), List.of())
                ))
                .toList();

        List<String> topImproved = summaries.stream()
                .filter(s -> expressionStatisticsService.isDataReady(s.getSessionCount()))
                .sorted(Comparator.comparingDouble(ExpressionStatSummary::getDurationDecreaseRate).reversed())
                .limit(3)
                .map(ExpressionStatSummary::getEmotionTarget)
                .toList();

        String message = null;
        if (includeEncouragementMessage) {
            // 장려 메시지용 이번 주 / 지난 주 세션을 여기서 한 번만 조회해서 넘긴다.
            ZoneId zone = ZoneId.of("Asia/Seoul");
            ZonedDateTime now = ZonedDateTime.now(zone);
            Instant thisWeekStart = now.with(statisticsProperties.getWeekStartDay())
                    .toLocalDate().atStartOfDay(zone).toInstant();
            Instant lastWeekStart = thisWeekStart.minus(Duration.ofDays(7));
            List<ExpressionSession> thisWeek = expressionSessionRepository
                    .findSessionsBetween(childId, thisWeekStart, now.toInstant());
            List<ExpressionSession> lastWeek = expressionSessionRepository
                    .findSessionsBetween(childId, lastWeekStart, thisWeekStart);
            message = buildEncouragementMessage(emotionStats, thisWeek, lastWeek);
        }

        return new ExpressionSummaryDto(childId, emotionStats, topImproved, message);
    }

    @Transactional(readOnly = true)
    public List<DialogueSummaryDto> buildDialogueSummaries(UUID childId) {
        // 테마별 주간 진행 트렌드 배치 조회 → Map으로 변환 (N+1 방지)
        Map<String, List<DialogueSummaryDto.WeeklyTrendDto>> trendByTheme =
                dialogueSessionRepository.findAllWeeklyProgressByChild(childId).stream()
                        .collect(Collectors.groupingBy(
                                DialogueAllThemeProgressProjection::getTheme,
                                Collectors.mapping(
                                        p -> new DialogueSummaryDto.WeeklyTrendDto(p.getWeekNumber(), p.getScoreRate()),
                                        Collectors.toList())));

        return dialogueStatSummaryRepository.findByChild_ChildId(childId).stream()
                .map(summary -> {
                    List<DialogueSummaryDto.WeeklyTrendDto> weeklyTrend =
                            trendByTheme.getOrDefault(summary.getTheme().getDisplayName(), List.of());
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
    public DialogueProgressDto buildDialogueProgress(UUID childId, ViewerRole viewerRole) {
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
                        viewerRole))
                .toList();

        return new DialogueProgressDto(childId, items);
    }

    @Transactional(readOnly = true)
    public WeeklyParticipationDto buildWeeklyParticipation(UUID childId) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime weekStart = now.with(statisticsProperties.getWeekStartDay()).toLocalDate().atStartOfDay(zone);
        ZonedDateTime weekEnd = weekStart.plusDays(7);

        Instant from = weekStart.toInstant();
        Instant to = weekEnd.toInstant();

        List<ExpressionSession> exprSessions = expressionSessionRepository.findSessionsBetween(childId, from, to);
        List<DialogueSession> dialogueSessions = dialogueSessionRepository.findSessionsBetween(childId, from, to);
        List<HomeworkReport> homeworkReports = offlineMissionStatisticsService.findReportsBetween(
                childId, weekStart.toLocalDateTime(), weekEnd.toLocalDateTime());

        Set<LocalDate> gamePlayedDates = new HashSet<>();
        exprSessions.forEach(s -> gamePlayedDates.add(s.getStartedAt().atZone(zone).toLocalDate()));
        dialogueSessions.forEach(s -> gamePlayedDates.add(s.getStartedAt().atZone(zone).toLocalDate()));

        Set<LocalDate> offlineMissionDates = homeworkReports.stream()
                .map(report -> report.getReportedAt().toLocalDate())
                .collect(Collectors.toSet());

        Set<LocalDate> playedDates = new HashSet<>(gamePlayedDates);
        playedDates.addAll(offlineMissionDates);

        int completedDays = playedDates.size();
        int gameCompletedDays = gamePlayedDates.size();
        int offlineMissionCompletedDays = offlineMissionDates.size();
        int recommended = statisticsProperties.getRecommendedPerWeek();
        boolean goalAchieved = completedDays >= recommended;

        List<Boolean> dayMarkers = new ArrayList<>();
        LocalDate monday = weekStart.toLocalDate();
        for (int i = 0; i < 7; i++) {
            dayMarkers.add(playedDates.contains(monday.plusDays(i)));
        }

        String message = goalAchieved
                ? "이번 주 목표를 모두 완료했어요."
                : String.format("이번 주 학습: 권장 %d일 중 %d일 완료", recommended, completedDays);

        return new WeeklyParticipationDto(
                childId,
                completedDays,
                gameCompletedDays,
                offlineMissionCompletedDays,
                recommended,
                goalAchieved,
                message,
                dayMarkers);
    }

    @Transactional(readOnly = true)
    public WeeklyHighlightDto buildWeeklyHighlight(UUID childId) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zone);
        Instant weekStart = now.with(statisticsProperties.getWeekStartDay()).toLocalDate().atStartOfDay(zone).toInstant();

        List<ExpressionSession> exprSessions = expressionSessionRepository
                .findSessionsBetween(childId, weekStart, now.toInstant());
        List<DialogueSession> dialogueSessions = dialogueSessionRepository
                .findSessionsBetween(childId, weekStart, now.toInstant());

        // 감정별/테마별 이번 주 이전 최고 기록을 배치 조회 (N+1 방지)
        Map<String, Float> exprBestMap = expressionSessionRepository
                .findBestAccuracyPerEmotionBefore(childId, weekStart)
                .stream()
                .collect(Collectors.toMap(
                        ExpressionBestAccuracyProjection::getEmotionTarget,
                        p -> p.getBestAccuracy() != null ? p.getBestAccuracy() : 0f));

        Map<PeersTheme, Float> dialogueBestMap = dialogueSessionRepository
                .findBestScoreRatePerThemeBefore(childId, weekStart)
                .stream()
                .collect(Collectors.toMap(
                        DialogueBestScoreProjection::getTheme,
                        p -> p.getBestScoreRate() != null ? p.getBestScoreRate() : 0f));

        List<String> highlights = new ArrayList<>();

        for (ExpressionSession session : exprSessions) {
            float prevBest = exprBestMap.getOrDefault(session.getEmotionTarget(), 0f);
            if (session.getFinalAccuracy() != null && session.getFinalAccuracy() > prevBest) {
                highlights.add(String.format("%s 표정에서 최고 기록을 세웠어요.", session.getEmotionTarget()));
            }
            if (session.getTotalTries() != null
                    && session.getTotalTries() == 1
                    && Boolean.TRUE.equals(session.getIsSuccess())) {
                highlights.add(String.format("%s 표정을 한 번에 성공했어요.", session.getEmotionTarget()));
            }
        }

        for (DialogueSession session : dialogueSessions) {
            float prevBest = dialogueBestMap.getOrDefault(session.getTheme(), 0f);
            if (session.getScoreRate() != null && session.getScoreRate() > prevBest) {
                highlights.add(String.format("%s에서 새 최고 점수를 받았어요.", session.getTheme().getDisplayName()));
            }
            if (hasConsecutivePerfectTurns(session.getTurns(), 2)) {
                highlights.add("대화에서 연속으로 좋은 선택을 했어요.");
            }
        }

        List<String> deduped = highlights.stream().distinct().limit(3).toList();
        String fallback = deduped.isEmpty() ? "이번 주도 꾸준히 참여하고 있어요. 충분히 해내고 있어요." : null;
        return new WeeklyHighlightDto(childId, deduped, fallback);
    }

    private DialogueProgressDto.ThemeProgressItem buildProgressItem(
            PeersTheme theme,
            DialogueStatSummary summary,
            int weekSessionCount,
            DialogueProgressDto.OfflineMissionProgressDto offlineProgress,
            ViewerRole viewerRole
    ) {
        if (summary == null) {
            String status = weekSessionCount > 0 ? "IN_PROGRESS" : "NOT_STARTED";
            String label = weekSessionCount > 0 ? "진행 중" : "미시작";
            return new DialogueProgressDto.ThemeProgressItem(
                    theme.getWeekNumber(), theme.getDisplayName(),
                    status, label, viewerRole.isTherapist() ? label : null,
                    weekSessionCount, null, null, offlineProgress, false);
        }

        double emaThreshold = statisticsProperties.getMastery().getEmaThreshold();
        int minSessions = statisticsProperties.getMastery().getMinSessions();

        boolean mastered = summary.getEmaValue() != null
                && summary.getEmaValue() >= emaThreshold
                && summary.getSessionCount() >= minSessions;

        // 게임 통계는 기준 미달이지만 오프라인에서 자발적 수행이 충분히 쌓인 경우 "현실 일반화"로 인정
        int offlineReviewed = summary.getOfflineReviewedCount() != null ? summary.getOfflineReviewedCount() : 0;
        boolean offlineGeneralized = !mastered
                && offlineReviewed >= 2
                && summary.getOfflineSpontaneousRate() != null
                && summary.getOfflineSpontaneousRate() >= 0.5;

        String status = mastered ? "COMPLETED" : offlineGeneralized ? "GENERALIZED" : "IN_PROGRESS";
        String parentLabel = mastered ? "완료" : offlineGeneralized ? "생활에서 연습 중" : "진행 중";

        String therapistLabel = null;
        if (viewerRole.isTherapist()) {
            if (summary.getEmaValue() == null) {
                therapistLabel = "데이터 부족";
            } else {
                String masteryTag = mastered ? "도달" : offlineGeneralized ? "현실 일반화" : "미숙달";
                if (offlineReviewed > 0) {
                    therapistLabel = String.format("%s (EMA %.2f, 미션자발 %.0f%%)",
                            masteryTag, summary.getEmaValue(),
                            (summary.getOfflineSpontaneousRate() != null ? summary.getOfflineSpontaneousRate() : 0.0) * 100);
                } else {
                    therapistLabel = String.format("%s (EMA %.2f)", masteryTag, summary.getEmaValue());
                }
            }
        }

        return new DialogueProgressDto.ThemeProgressItem(
                theme.getWeekNumber(),
                theme.getDisplayName(),
                status,
                parentLabel,
                therapistLabel,
                summary.getSessionCount(),
                summary.getEmaValue(),
                summary.getConsistencyStd(),
                offlineProgress,
                offlineGeneralized
        );
    }

    private String resolveGraphPhase(int weekCount) {
        if (weekCount >= statisticsProperties.getGraphPhase().getCompleteWeeks()) return "COMPLETE";
        if (weekCount >= statisticsProperties.getGraphPhase().getMidWeeks()) return "MID";
        return "EARLY";
    }

    private boolean hasConsecutivePerfectTurns(List<DialogueTurn> turns, int minConsecutive) {
        if (turns == null) return false;
        int count = 0;
        for (DialogueTurn t : turns) {
            if (t.getSelectedScore() == 2) {
                count++;
                if (count >= minConsecutive) return true;
            } else {
                count = 0;
            }
        }
        return false;
    }

    private String buildEncouragementMessage(
            List<ExpressionSummaryDto.EmotionStatDto> emotionStats,
            List<ExpressionSession> thisWeek,
            List<ExpressionSession> lastWeek) {
        long readyCount = emotionStats.stream().filter(ExpressionSummaryDto.EmotionStatDto::dataReady).count();
        if (readyCount == 0) {
            return "기록이 쌓이는 중이에요. 조금만 더 참여하면 변화가 보여요.";
        }

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
