package myexpressionfriend_api.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.child.repository.ChildrenAuthorizedUserRepository;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.homework.domain.HomeworkAssignment;
import myexpressionfriend_api.homework.repository.HomeworkAssignmentRepository;
import myexpressionfriend_api.homework.service.HomeworkService;
import myexpressionfriend_api.notification.domain.NotificationMessages;
import myexpressionfriend_api.notification.domain.NotificationType;
import myexpressionfriend_api.notification.preference.domain.NotificationPreference;
import myexpressionfriend_api.notification.preference.domain.NotificationPreferenceType;
import myexpressionfriend_api.notification.preference.repository.NotificationPreferenceRepository;
import myexpressionfriend_api.notification.repository.NotificationRepository;
import myexpressionfriend_api.notification.service.NotificationService;
import myexpressionfriend_api.statistics.dashboard.service.DashboardSummaryAssembler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private static final int DEFAULT_INACTIVE_DAYS = 7;

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final ChildRepository childRepository;
    private final ChildrenAuthorizedUserRepository authorizedUserRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
    private final DashboardSummaryAssembler dashboardSummaryAssembler;

    // ─── 주간 성장 요약 (매주 월요일 오전 9시) ──────────────────────────────

    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklySummary() {
        log.info("[스케줄러] 주간 성장 요약 알림 발송 시작");

        List<NotificationPreference> prefs =
                preferenceRepository.findByPreferenceTypeAndEnabledTrue(NotificationPreferenceType.WEEKLY_SUMMARY);

        for (NotificationPreference pref : prefs) {
            UUID parentId = pref.getUserId();
            List<Child> children = childRepository.findByPrimaryParentUserId(parentId);

            for (Child child : children) {
                UUID childId = child.getChildId();

                // 중복 방지: 6일 이내 동일 아동에게 이미 발송했으면 스킵
                boolean alreadySent = notificationRepository
                        .existsByReceiverUserIdAndNotificationTypeAndReferenceIdAndCreatedAtAfter(
                                parentId,
                                NotificationType.WEEKLY_SUMMARY,
                                childId,
                                LocalDateTime.now().minusDays(6)
                        );

                if (alreadySent) {
                    log.debug("[주간요약] 중복 스킵 parentId={}, childId={}", parentId, childId);
                    continue;
                }

                try {
                    String topHighlight = resolveTopHighlight(childId);
                    NotificationMessages.Message msg = NotificationMessages.weeklySummary(child.getName(), topHighlight);
                    notificationService.saveAndSend(
                            parentId,
                            NotificationType.WEEKLY_SUMMARY,
                            msg.title(),
                            msg.body(),
                            childId
                    );
                    log.info("[주간요약] 발송 완료 parentId={}, childId={}", parentId, childId);
                } catch (Exception e) {
                    log.error("[주간요약] 발송 실패 parentId={}, childId={}: {}", parentId, childId, e.getMessage());
                }
            }
        }

        log.info("[스케줄러] 주간 성장 요약 알림 발송 완료 (대상 보호자 수={})", prefs.size());
    }

    // ─── 아동 미접속 알림 (매일 오전 9시) ───────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    public void checkChildInactivity() {
        log.info("[스케줄러] 아동 미접속 알림 체크 시작");

        List<NotificationPreference> prefs =
                preferenceRepository.findByPreferenceTypeAndEnabledTrue(NotificationPreferenceType.CHILD_INACTIVE);

        for (NotificationPreference pref : prefs) {
            UUID therapistId = pref.getUserId();
            int inactiveDays = pref.getExtraValue() != null ? pref.getExtraValue() : DEFAULT_INACTIVE_DAYS;

            List<Child> children = childRepository.findAccessibleByUserId(therapistId);

            for (Child child : children) {
                UUID childId = child.getChildId();
                Instant threshold = Instant.now().minus(inactiveDays, ChronoUnit.DAYS);

                // 마지막 플레이 시각 = dialogue/expression 중 최신값
                Instant lastPlayed = resolveLastPlayedAt(childId);

                boolean inactive = (lastPlayed == null || lastPlayed.isBefore(threshold));
                if (!inactive) continue;

                // 중복 방지: 오늘 이미 발송했으면 스킵
                boolean alreadySent = notificationRepository
                        .existsByReceiverUserIdAndNotificationTypeAndReferenceIdAndCreatedAtAfter(
                                therapistId,
                                NotificationType.CHILD_INACTIVE,
                                childId,
                                LocalDateTime.now().minusHours(23)
                        );

                if (alreadySent) {
                    log.debug("[미접속] 중복 스킵 therapistId={}, childId={}", therapistId, childId);
                    continue;
                }

                try {
                    NotificationMessages.Message msg =
                            NotificationMessages.childInactive(child.getName(), inactiveDays, lastPlayed != null);
                    notificationService.saveAndSend(
                            therapistId,
                            NotificationType.CHILD_INACTIVE,
                            msg.title(),
                            msg.body(),
                            childId
                    );
                    log.info("[미접속] 알림 발송 therapistId={}, childId={}, inactiveDays={}", therapistId, childId, inactiveDays);
                } catch (Exception e) {
                    log.error("[미접속] 알림 발송 실패 therapistId={}, childId={}: {}", therapistId, childId, e.getMessage());
                }
            }
        }

        log.info("[스케줄러] 아동 미접속 알림 체크 완료 (대상 치료사 수={})", prefs.size());
    }

    // ─── 기한 초과 숙제 자동 만료 (매일 자정) ──────────────────────────

    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueHomework() {
        log.info("[스케줄러] 기한 초과 숙제 만료 처리 시작");
        LocalDate today = LocalDate.now();
        List<HomeworkAssignment> expired = homeworkAssignmentRepository.findExpiredPending(today);

        for (HomeworkAssignment homework : expired) {
            try {
                homework.expire();
                homeworkAssignmentRepository.save(homework);

                // VIEW_REPORT 권한을 가진 보호자에게 만료 알림
                String label = HomeworkService.strategyFocusLabel(homework.getStrategyFocus());
                String childName = homework.getChild().getName();
                NotificationMessages.Message msg = NotificationMessages.homeworkExpired(childName, label);
                authorizedUserRepository
                        .findByChildIdAndPermission(homework.getChild().getChildId(), ChildPermissionType.WRITE_NOTE)
                        .forEach(au -> {
                            try {
                                notificationService.saveAndSend(
                                        au.getUser().getUserId(),
                                        NotificationType.HOMEWORK_EXPIRED,
                                        msg.title(),
                                        msg.body(),
                                        homework.getHomeworkId()
                                );
                            } catch (Exception e) {
                                log.warn("[만료] 알림 전송 실패 userId={}: {}", au.getUser().getUserId(), e.getMessage());
                            }
                        });

                log.info("[만료] 처리 완료 homeworkId={}", homework.getHomeworkId());
            } catch (Exception e) {
                log.error("[만료] 처리 실패 homeworkId={}: {}", homework.getHomeworkId(), e.getMessage());
            }
        }

        log.info("[스케줄러] 기한 초과 숙제 만료 처리 완료 (처리 건수={})", expired.size());
    }

    // ─── private ──────────────────────────────────────────────────────

    /**
     * 이번 주 하이라이트 중 첫 번째 항목을 반환한다. 없으면 null.
     */
    private String resolveTopHighlight(UUID childId) {
        try {
            var highlights = dashboardSummaryAssembler.buildWeeklyHighlight(childId).highlights();
            return (highlights != null && !highlights.isEmpty()) ? highlights.get(0) : null;
        } catch (Exception e) {
            log.warn("[주간요약] 하이라이트 조회 실패 childId={}: {}", childId, e.getMessage());
            return null;
        }
    }

    /**
     * 아동의 마지막 게임 플레이 시각 (dialogue + expression 중 최신)
     */
    private Instant resolveLastPlayedAt(UUID childId) {
        Optional<Instant> lastDialogue = dialogueSessionRepository.findLastPlayedAt(childId);
        Optional<Instant> lastExpression = expressionSessionRepository.findLastPlayedAt(childId);

        if (lastDialogue.isEmpty() && lastExpression.isEmpty()) return null;
        if (lastDialogue.isEmpty()) return lastExpression.get();
        if (lastExpression.isEmpty()) return lastDialogue.get();

        return lastDialogue.get().isAfter(lastExpression.get())
                ? lastDialogue.get()
                : lastExpression.get();
    }
}
