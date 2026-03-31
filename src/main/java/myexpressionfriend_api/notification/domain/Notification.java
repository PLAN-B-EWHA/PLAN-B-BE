package myexpressionfriend_api.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_receiver", columnList = "receiver_user_id"),
        @Index(name = "idx_notifications_receiver_created", columnList = "receiver_user_id, created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID notificationId;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID receiverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "uuid")
    private UUID referenceId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── 도메인 메서드 ──────────────────────────────────────────────────

    public void markAsRead() {
        this.isRead = true;
    }

    // ─── 팩토리 메서드 ─────────────────────────────────────────────────

    public static Notification create(UUID receiverUserId, NotificationType type,
                                      String title, String message, UUID referenceId) {
        return Notification.builder()
                .receiverUserId(receiverUserId)
                .notificationType(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .build();
    }
}
