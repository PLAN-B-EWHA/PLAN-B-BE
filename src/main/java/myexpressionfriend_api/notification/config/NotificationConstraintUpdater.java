package myexpressionfriend_api.notification.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.notification.domain.NotificationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 애플리케이션 시작 시 notifications.notification_type CHECK 제약 조건을
 * NotificationType enum 값과 동기화한다.
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
            entityManager.createNativeQuery(
                    "ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check"
            ).executeUpdate();

            String allowedTypes = Arrays.stream(NotificationType.values())
                    .map(NotificationType::name)
                    .map(v -> "'" + v + "'")
                    .collect(Collectors.joining(","));

            String sql = "ALTER TABLE notifications ADD CONSTRAINT notifications_notification_type_check " +
                    "CHECK (notification_type IN (" + allowedTypes + "))";

            entityManager.createNativeQuery(sql).executeUpdate();
            log.info("notifications.notification_type CHECK constraint synced: {}", allowedTypes);
        } catch (Exception e) {
            log.warn("notifications.notification_type CHECK constraint sync failed: {}", e.getMessage());
        }
    }
}
