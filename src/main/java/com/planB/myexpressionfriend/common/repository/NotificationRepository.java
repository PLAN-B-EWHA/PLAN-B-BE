package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByReceiverUserIdOrderByCreatedAtDesc(UUID receiverUserId, Pageable pageable);

    Optional<Notification> findByNotificationIdAndReceiverUserId(UUID notificationId, UUID receiverUserId);
}
