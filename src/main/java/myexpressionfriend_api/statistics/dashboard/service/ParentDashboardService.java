package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.config.StatisticsProperties;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
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
public class ParentDashboardService {

    private final ChildRepository childRepository;
    private final ExpressionStatSummaryRepository expressionStatSummaryRepository;
    private final DialogueStatSummaryRepository dialogueStatSummaryRepository;
    private final ExpressionStatisticsService expressionStatisticsService;
    private final DialogueStatisticsService dialogueStatisticsService;
    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
    private final StatisticsProperties statisticsProperties;
    private final OfflineMissionStatisticsService offlineMissionStatisticsService;

    @Transactional(readOnly = true)
    public ExpressionSummaryDto getExpressionSummary(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);

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

        return new ExpressionSummaryDto(childId, emotionStats, topImproved, buildEncouragementMessage(childId, emotionStats));
    }

    @Transactional(readOnly = true)
    public List<DialogueSummaryDto> getAllDialogueSummaries(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);

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
                            summary.getRetryReductionRate()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyParticipationDto getWeeklyParticipation(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);

        ZoneId zone = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime weekStart = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(zone);
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
    public WeeklyHighlightDto getWeeklyHighlight(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);

        ZoneId zone = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime weekStart = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(zone);

        List<ExpressionSession> exprSessions = expressionSessionRepository
                .findSessionsBetween(childId, weekStart.toInstant(), now.toInstant());
        List<DialogueSession> dialogueSessions = dialogueSessionRepository
                .findSessionsBetween(childId, weekStart.toInstant(), now.toInstant());

        List<String> highlights = new ArrayList<>();

        for (ExpressionSession session : exprSessions) {
            float prevBest = expressionSessionRepository.findPersonalBestAccuracy(
                    childId, session.getEmotionTarget(), session.getSessionId());
            if (session.getFinalAccuracy() != null && session.getFinalAccuracy() > prevBest) {
                highlights.add(String.format("%s 표정에서 최고 기록을 달성했어요.", session.getEmotionTarget()));
            }
            if (session.getTotalTries() != null && session.getTotalTries() == 1 && Boolean.TRUE.equals(session.getIsSuccess())) {
                highlights.add(String.format("%s 표정을 한 번에 성공했어요.", session.getEmotionTarget()));
            }
        }

        for (DialogueSession session : dialogueSessions) {
            float prevBest = dialogueSessionRepository.findPersonalBestScoreRate(childId, session.getSessionId());
            if (session.getScoreRate() != null && session.getScoreRate() > prevBest) {
                highlights.add(String.format("%s에서 새 최고 점수를 받았어요.", session.getTheme().getDisplayName()));
            }
            if (hasConsecutivePerfectTurns(session.getTurns(), 2)) {
                highlights.add("대화에서 연속으로 좋은 선택을 했어요.");
            }
        }

        List<String> deduped = highlights.stream().distinct().limit(3).toList();
        String fallback = deduped.isEmpty() ? "이번 주도 꾸준히 참여하고 있어요. 충분히 잘하고 있어요." : null;
        return new WeeklyHighlightDto(childId, deduped, fallback);
    }

    @Transactional(readOnly = true)
    public DialogueProgressDto getDialogueProgress(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);

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
                .map(theme -> {
                    DialogueStatSummary summary = summaryMap.get(theme);
                    int weekSessionCount = playedWeekSessionCount.getOrDefault(theme.getWeekNumber(), 0L).intValue();
                    DialogueProgressDto.OfflineMissionProgressDto offlineProgress =
                            offlineProgressByWeek.get(theme.getWeekNumber());

                    if (summary == null) {
                        if (weekSessionCount > 0) {
                            return new DialogueProgressDto.ThemeProgressItem(
                                    theme.getWeekNumber(), theme.getDisplayName(),
                                    "IN_PROGRESS", "진행 중", "진행 중",
                                    weekSessionCount, null, null, offlineProgress);
                        }
                        return new DialogueProgressDto.ThemeProgressItem(
                                theme.getWeekNumber(), theme.getDisplayName(),
                                "NOT_STARTED", "미시작", "미시작",
                                0, null, null, offlineProgress);
                    }

                    boolean mastered = summary.getEmaValue() != null
                            && summary.getEmaValue() >= 0.65
                            && summary.getSessionCount() >= 3;

                    return new DialogueProgressDto.ThemeProgressItem(
                            theme.getWeekNumber(), theme.getDisplayName(),
                            mastered ? "COMPLETED" : "IN_PROGRESS",
                            mastered ? "완료" : "진행 중",
                            null,
                            summary.getSessionCount(),
                            summary.getEmaValue(),
                            summary.getConsistencyStd(),
                            offlineProgress);
                })
                .toList();

        return new DialogueProgressDto(childId, items);
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

    private String resolveGraphPhase(int weekCount) {
        if (weekCount >= 16) return "COMPLETE";
        if (weekCount >= 9) return "MID";
        return "EARLY";
    }

    protected Child loadChildWithPermissionCheck(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동 정보를 찾을 수 없습니다."));
        if (!child.hasPermission(userId, ChildPermissionType.VIEW_REPORT)) {
            throw new AuthenticationFailedException("리포트 조회 권한이 없습니다.");
        }
        return child;
    }

    private String buildEncouragementMessage(UUID childId, List<ExpressionSummaryDto.EmotionStatDto> emotionStats) {
        long readyCount = emotionStats.stream().filter(ExpressionSummaryDto.EmotionStatDto::dataReady).count();
        if (readyCount == 0) {
            return "기록을 쌓는 중이에요. 조금만 더 참여하면 변화가 보여요.";
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
                return "이번 주도 꾸준히 참여하고 있어요. 충분히 잘하고 있어요.";
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
