package myexpressionfriend_api.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.notification.domain.NotificationType;
import myexpressionfriend_api.notification.preference.domain.NotificationPreference;
import myexpressionfriend_api.notification.preference.domain.NotificationPreferenceType;
import myexpressionfriend_api.notification.preference.repository.NotificationPreferenceRepository;
import myexpressionfriend_api.notification.repository.NotificationRepository;
import myexpressionfriend_api.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;

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
                    notificationService.saveAndSend(
                            parentId,
                            NotificationType.WEEKLY_SUMMARY,
                            "이번 주 " + child.getName() + " 아동의 성장 요약이 도착했습니다",
                            child.getName() + " 아동의 이번 주 대화·표정 게임 활동 요약을 확인해 보세요.",
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
                    String lastPlayedDesc = lastPlayed == null
                            ? "아직 게임 기록이 없습니다"
                            : inactiveDays + "일 이상 접속하지 않았습니다";

                    notificationService.saveAndSend(
                            therapistId,
                            NotificationType.CHILD_INACTIVE,
                            child.getName() + " 아동이 " + inactiveDays + "일째 미접속 중입니다",
                            child.getName() + " 아동이 " + lastPlayedDesc + ". 확인해 보세요.",
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

    // ─── private ──────────────────────────────────────────────────────

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
