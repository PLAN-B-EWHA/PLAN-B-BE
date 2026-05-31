package myexpressionfriend_api.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildrenAuthorizedUserRepository;
import myexpressionfriend_api.homework.event.HomeworkReviewedEvent;
import myexpressionfriend_api.homework.event.HomeworkSubmittedEvent;
import myexpressionfriend_api.homework.service.HomeworkService;
import myexpressionfriend_api.notification.domain.NotificationMessages;
import myexpressionfriend_api.notification.domain.NotificationType;
import myexpressionfriend_api.report.event.ReportPublishedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final ChildrenAuthorizedUserRepository authorizedUserRepository;

    // ─── 리포트 발행 → 모든 VIEW_REPORT 보호자에게 알림 ─────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportPublished(ReportPublishedEvent event) {
        NotificationMessages.Message msg = NotificationMessages.reportGenerated(event.childName());
        notifyAll(
                authorizedUserRepository.findByChildIdAndPermission(event.childId(), ChildPermissionType.VIEW_REPORT),
                NotificationType.REPORT_GENERATED, msg, event.reportId()
        );
    }

    // ─── 숙제 제출 → 치료사(ASSIGN_MISSION 권한)에게 알림 ───────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHomeworkSubmitted(HomeworkSubmittedEvent event) {
        String label = HomeworkService.strategyFocusLabel(event.strategyFocus());
        NotificationMessages.Message msg = NotificationMessages.homeworkSubmitted(event.childName(), label);
        notifyAll(
                authorizedUserRepository.findByChildIdAndPermission(event.childId(), ChildPermissionType.ASSIGN_MISSION),
                NotificationType.HOMEWORK_SUBMITTED, msg, event.homeworkId()
        );
    }

    // ─── 숙제 검토 완료 → 제출한 보호자에게 알림 ────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHomeworkReviewed(HomeworkReviewedEvent event) {
        String label = HomeworkService.strategyFocusLabel(event.strategyFocus());
        NotificationMessages.Message msg = NotificationMessages.homeworkReviewed(event.childName(), label);
        try {
            notificationService.saveAndSend(
                    event.reportedByUserId(),
                    NotificationType.HOMEWORK_REVIEWED,
                    msg.title(),
                    msg.body(),
                    event.homeworkId()
            );
        } catch (Exception e) {
            log.warn("숙제 검토 알림 전송 실패 homeworkId={}: {}", event.homeworkId(), e.getMessage());
        }
    }

    // ─── 공통 헬퍼 ───────────────────────────────────────────────────────

    private void notifyAll(
            java.util.List<myexpressionfriend_api.child.domain.ChildrenAuthorizedUser> targets,
            NotificationType type,
            NotificationMessages.Message msg,
            java.util.UUID referenceId
    ) {
        for (var target : targets) {
            try {
                notificationService.saveAndSend(
                        target.getUser().getUserId(),
                        type,
                        msg.title(),
                        msg.body(),
                        referenceId
                );
            } catch (Exception e) {
                log.warn("알림 전송 실패 type={}, userId={}: {}",
                        type, target.getUser().getUserId(), e.getMessage());
            }
        }
    }
}
