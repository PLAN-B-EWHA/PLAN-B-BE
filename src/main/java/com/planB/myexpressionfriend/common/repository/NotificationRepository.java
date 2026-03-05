package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.notification.Notification;
import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByReceiverUserIdOrderByCreatedAtDesc(UUID receiverUserId, Pageable pageable);

    Optional<Notification> findByNotificationIdAndReceiverUserId(UUID notificationId, UUID receiverUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.receiverUserId = :receiverUserId
              AND n.isRead = false
            """)
    int markAllAsReadByReceiverUserId(@Param("receiverUserId") UUID receiverUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.receiverUserId = :receiverUserId
              AND n.type = :type
              AND n.isRead = false
            """)
    int markAllAsReadByReceiverUserIdAndType(
            @Param("receiverUserId") UUID receiverUserId,
            @Param("type") NotificationType type
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Notification n
            WHERE n.receiverUserId = :receiverUserId
              AND n.createdAt < :before
            """)
    int deleteByReceiverUserIdAndCreatedAtBefore(
            @Param("receiverUserId") UUID receiverUserId,
            @Param("before") java.time.LocalDateTime before
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Notification n
            WHERE n.receiverUserId = :receiverUserId
              AND n.type = :type
              AND n.createdAt < :before
            """)
    int deleteByReceiverUserIdAndTypeAndCreatedAtBefore(
            @Param("receiverUserId") UUID receiverUserId,
            @Param("type") NotificationType type,
            @Param("before") java.time.LocalDateTime before
    );
}
