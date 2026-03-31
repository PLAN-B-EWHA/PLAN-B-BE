package myexpressionfriend_api.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.dto.common.PageResponseDTO;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.notification.domain.Notification;
import myexpressionfriend_api.notification.domain.NotificationType;
import myexpressionfriend_api.notification.dto.NotificationDTO;
import myexpressionfriend_api.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** 사용자별 SSE Emitter 레지스트리 (userId → SseEmitter) */
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ─── SSE 구독 ──────────────────────────────────────────────────────

    public SseEmitter subscribe(UUID userId) {
        // 기존 emitter가 있으면 완료 처리
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) {
            try { existing.complete(); } catch (Exception ignored) {}
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        emitters.put(userId, emitter);

        // 연결 확인 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            log.warn("SSE 초기 이벤트 전송 실패 userId={}: {}", userId, e.getMessage());
            emitters.remove(userId);
            emitter.complete();
        }

        return emitter;
    }

    // ─── 알림 저장 & 실시간 전송 ────────────────────────────────────────

    /**
     * 새 트랜잭션으로 알림을 저장하고 SSE로 실시간 전송합니다.
     * 호출자 트랜잭션과 분리하여 커밋 후 SSE 전송을 보장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDTO saveAndSend(UUID receiverUserId, NotificationType type,
                                       String title, String message, UUID referenceId) {
        Notification notification = Notification.create(receiverUserId, type, title, message, referenceId);
        notification = notificationRepository.save(notification);
        NotificationDTO dto = NotificationDTO.from(notification);
        sendRealtime(receiverUserId, dto);
        return dto;
    }

    /**
     * SSE 실시간 전송 (emitter가 없으면 무시)
     */
    private void sendRealtime(UUID userId, NotificationDTO dto) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(dto));
        } catch (IOException e) {
            log.warn("SSE 알림 전송 실패 userId={}: {}", userId, e.getMessage());
            emitters.remove(userId);
        }
    }

    // ─── 조회 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponseDTO<NotificationDTO> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findByReceiverUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponseDTO.from(page, NotificationDTO::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByReceiverUserIdAndIsReadFalse(userId);
    }

    // ─── 읽음 처리 ─────────────────────────────────────────────────────

    @Transactional
    public void markAsRead(UUID notificationId, UUID requestUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다. id=" + notificationId));
        if (!notification.getReceiverUserId().equals(requestUserId)) {
            throw new InvalidRequestException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        notification.markAsRead();
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    // ─── 정리 ──────────────────────────────────────────────────────────

    @Transactional
    public int deleteOldNotifications(LocalDateTime before) {
        return notificationRepository.deleteOldNotifications(before);
    }
}
