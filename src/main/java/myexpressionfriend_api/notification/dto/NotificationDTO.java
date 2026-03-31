package myexpressionfriend_api.notification.dto;

import myexpressionfriend_api.notification.domain.Notification;
import myexpressionfriend_api.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO(
        UUID notificationId,
        UUID receiverUserId,
        NotificationType notificationType,
        String title,
        String message,
        UUID referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationDTO from(Notification notification) {
        return new NotificationDTO(
                notification.getNotificationId(),
                notification.getReceiverUserId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
