package myexpressionfriend_api.notification.repository;

import myexpressionfriend_api.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import myexpressionfriend_api.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * 중복 발송 방지: 동일 수신자·타입·참조ID로 특정 시각 이후에 보낸 알림이 존재하는지 확인
     */
    boolean existsByReceiverUserIdAndNotificationTypeAndReferenceIdAndCreatedAtAfter(
            UUID receiverUserId,
            NotificationType notificationType,
            UUID referenceId,
            LocalDateTime after
    );


    Page<Notification> findByReceiverUserIdOrderByCreatedAtDesc(UUID receiverUserId, Pageable pageable);

    long countByReceiverUserIdAndIsReadFalse(UUID receiverUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiverUserId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    int deleteOldNotifications(@Param("before") LocalDateTime before);
}
