package myexpressionfriend_api.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트 → 알림 변환 리스너.
 *
 * <p>관련 도메인(미션, 리포트, 노트 등)이 이 프로젝트에 구현되면
 * 아래와 같이 핸들러를 추가하세요:</p>
 *
 * <pre>{@code
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * public void handleMissionCompleted(MissionCompletedEvent event) {
 *     notificationService.saveAndSend(
 *         event.getParentUserId(),
 *         NotificationType.MISSION_COMPLETED,
 *         "미션 완료",
 *         event.getChildName() + "님이 미션을 완료했습니다.",
 *         event.getMissionId()
 *     );
 * }
 * }</pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    // 향후 이벤트 핸들러 추가 예정
    // 현재 프로젝트에 해당 도메인 이벤트가 없으므로 비어 있습니다.
}
