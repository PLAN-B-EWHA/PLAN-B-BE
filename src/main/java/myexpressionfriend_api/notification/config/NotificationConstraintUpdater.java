package myexpressionfriend_api.notification.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 notifications.notification_type 컬럼의
 * CHECK 제약 조건을 현재 {@link myexpressionfriend_api.notification.domain.NotificationType} 값과 동기화합니다.
 *
 * <p>새 NotificationType 값이 추가되면 이 클래스의 문자열 목록도 함께 업데이트하세요.</p>
 */
@Component
@Slf4j
public class NotificationConstraintUpdater {

    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void updateNotificationTypeConstraint() {
        try {
            // 기존 제약 조건 제거 (없으면 무시)
            entityManager.createNativeQuery(
                    "ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check"
            ).executeUpdate();

            // 현재 enum 값으로 제약 조건 재생성
            entityManager.createNativeQuery(
                    "ALTER TABLE notifications ADD CONSTRAINT notifications_notification_type_check " +
                    "CHECK (notification_type IN (" +
                    "'MISSION_COMPLETED'," +
                    "'MISSION_PHOTO_UPLOADED'," +
                    "'REPORT_GENERATED'," +
                    "'NOTE_COMMENT_ADDED'," +
                    "'NOTE_REPLY_ADDED'," +
                    "'NOTE_ASSET_UPLOADED'" +
                    "))"
            ).executeUpdate();

            log.info("notifications.notification_type CHECK 제약 조건 업데이트 완료");
        } catch (Exception e) {
            // notifications 테이블이 아직 생성되지 않았거나 DDL 권한이 없는 경우 등
            log.warn("notifications.notification_type CHECK 제약 조건 업데이트 실패: {}", e.getMessage());
        }
    }
}
