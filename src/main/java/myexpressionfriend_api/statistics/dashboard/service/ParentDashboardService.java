package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.config.StatisticsProperties;
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
import myexpressionfriend_api.statistics.dialogue.dto.DialogueSummaryDto;
import myexpressionfriend_api.statistics.expression.dto.ExpressionSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentDashboardService {

    private final ChildRepository childRepository;
    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
    private final StatisticsProperties statisticsProperties;
    private final OfflineMissionStatisticsService offlineMissionStatisticsService;
    private final DashboardSummaryAssembler dashboardSummaryAssembler;

    @Transactional(readOnly = true)
    public ExpressionSummaryDto getExpressionSummary(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);
        return dashboardSummaryAssembler.buildExpressionSummary(childId, true);
    }

    @Transactional(readOnly = true)
    public List<DialogueSummaryDto> getAllDialogueSummaries(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);
        return dashboardSummaryAssembler.buildDialogueSummaries(childId);
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
                highlights.add(String.format("%s 표정에서 최고 기록을 세웠어요.", session.getEmotionTarget()));
            }
            if (session.getTotalTries() != null
                    && session.getTotalTries() == 1
                    && Boolean.TRUE.equals(session.getIsSuccess())) {
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
        String fallback = deduped.isEmpty() ? "이번 주도 꾸준히 참여하고 있어요. 충분히 해내고 있어요." : null;
        return new WeeklyHighlightDto(childId, deduped, fallback);
    }

    @Transactional(readOnly = true)
    public DialogueProgressDto getDialogueProgress(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);
        return dashboardSummaryAssembler.buildDialogueProgress(childId, false);
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

    protected Child loadChildWithPermissionCheck(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동 정보를 찾을 수 없습니다."));
        if (!child.hasPermission(userId, ChildPermissionType.VIEW_REPORT)) {
            throw new AuthenticationFailedException("리포트 조회 권한이 없습니다.");
        }
        return child;
    }
}
