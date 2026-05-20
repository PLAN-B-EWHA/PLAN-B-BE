package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TherapistDashboardService {

    private final ChildRepository childRepository;
    private final ExpressionStatSummaryRepository expressionStatSummaryRepository;
    private final DialogueStatSummaryRepository dialogueStatSummaryRepository;
    private final ExpressionStatisticsService expressionStatisticsService;
    private final DialogueStatisticsService dialogueStatisticsService;
    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
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

        return new ExpressionSummaryDto(childId, emotionStats, topImproved, null);
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
                    String therapistLabel = summary.getEmaValue() == null
                            ? "데이터 부족"
                            : String.format("%s (EMA %.2f)", mastered ? "숙달" : "미숙달", summary.getEmaValue());

                    return new DialogueProgressDto.ThemeProgressItem(
                            theme.getWeekNumber(),
                            theme.getDisplayName(),
                            mastered ? "COMPLETED" : "IN_PROGRESS",
                            mastered ? "완료" : "진행 중",
                            therapistLabel,
                            summary.getSessionCount(),
                            summary.getEmaValue(),
                            summary.getConsistencyStd(),
                            offlineProgress
                    );
                })
                .toList();

        return new DialogueProgressDto(childId, items);
    }

    private String resolveGraphPhase(int weekCount) {
        if (weekCount >= 16) return "COMPLETE";
        if (weekCount >= 9) return "MID";
        return "EARLY";
    }

    public Child loadChildWithPermissionCheck(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동 정보를 찾을 수 없습니다."));
        if (!child.hasPermission(userId, ChildPermissionType.VIEW_REPORT)) {
            throw new AuthenticationFailedException("리포트 조회 권한이 없습니다.");
        }
        return child;
    }
}
