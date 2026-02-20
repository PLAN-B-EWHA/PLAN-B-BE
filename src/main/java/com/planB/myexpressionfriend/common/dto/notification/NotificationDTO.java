package com.planB.myexpressionfriend.common.dto.notification;

import com.planB.myexpressionfriend.common.domain.notification.Notification;
import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationDTO {
    private UUID notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private UUID referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationDTO from(Notification notification) {
        return NotificationDTO.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
