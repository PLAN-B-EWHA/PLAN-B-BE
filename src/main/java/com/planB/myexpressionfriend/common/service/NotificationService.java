package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.notification.Notification;
import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import com.planB.myexpressionfriend.common.dto.notification.NotificationDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private final NotificationRepository notificationRepository;
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter oldEmitter = emitters.remove(userId);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(ex -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE connected"));
        } catch (IOException e) {
            emitters.remove(userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDTO saveAndSend(
            UUID receiverUserId,
            NotificationType type,
            String title,
            String message,
            UUID referenceId
    ) {
        try {
            Notification notification = Notification.builder()
                    .receiverUserId(receiverUserId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .referenceId(referenceId)
                    .isRead(false)
                    .build();

            Notification saved = notificationRepository.saveAndFlush(notification);
            NotificationDTO dto = NotificationDTO.from(saved);

            log.info("Notification saved. notificationId={}, receiverUserId={}, type={}, referenceId={}",
                    saved.getNotificationId(), receiverUserId, type, referenceId);

            sendRealtime(receiverUserId, "notification", dto);
            return dto;
        } catch (RuntimeException e) {
            log.error("Notification save failed. receiverUserId={}, type={}, referenceId={}",
                    receiverUserId, type, referenceId, e);
            throw e;
        }
    }

    public PageResponseDTO<NotificationDTO> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByReceiverUserIdOrderByCreatedAtDesc(userId, pageable);
        log.info("Notification list fetched. userId={}, page={}, size={}, totalElements={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements());
        return PageResponseDTO.from(page, NotificationDTO::from);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByNotificationIdAndReceiverUserId(notificationId, userId)
                .orElseThrow(() -> new AccessDeniedException("알림 접근 권한이 없거나 존재하지 않습니다."));
        notification.markAsRead();
    }

    public void sendHeartbeat(UUID userId) {
        sendRealtime(userId, "heartbeat", "ping");
    }

    private void sendRealtime(UUID userId, String eventName, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException e) {
            log.debug("SSE 전송 실패, emitter 제거 - userId: {}", userId);
            emitters.remove(userId);
            emitter.complete();
        }
    }
}
