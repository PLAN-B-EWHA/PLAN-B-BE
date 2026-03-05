package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.notification.Notification;
import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.notification.NotificationDTO;
import com.planB.myexpressionfriend.common.repository.AssignedMissionRepository;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.GeneratedReportRepository;
import com.planB.myexpressionfriend.common.repository.NoteCommentRepository;
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
import java.time.LocalDateTime;
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
    private final AssignedMissionRepository assignedMissionRepository;
    private final GeneratedReportRepository generatedReportRepository;
    private final ChildNoteRepository childNoteRepository;
    private final NoteCommentRepository noteCommentRepository;
    private final OpsMetricService opsMetricService;
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
            validateReceiverAuthorization(receiverUserId, type, referenceId);

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
            opsMetricService.incrementNotificationSaveFailure();
            log.error("Notification save failed. receiverUserId={}, type={}, referenceId={}",
                    receiverUserId, type, referenceId, e);
            throw e;
        }
    }

    private void validateReceiverAuthorization(UUID receiverUserId, NotificationType type, UUID referenceId) {
        if (receiverUserId == null) {
            throw new AccessDeniedException("수신자 정보가 없습니다.");
        }
        if (type == null) {
            throw new AccessDeniedException("알림 유형이 없습니다.");
        }
        if (referenceId == null) {
            throw new AccessDeniedException("참조 ID가 없습니다.");
        }

        boolean authorized = switch (type) {
            case MISSION_COMPLETED, MISSION_PHOTO_UPLOADED ->
                    assignedMissionRepository.findByIdWithAuth(referenceId, receiverUserId).isPresent();
            case REPORT_GENERATED ->
                    generatedReportRepository.findAuthorizedByReportId(referenceId, receiverUserId).isPresent();
            case NOTE_ASSET_UPLOADED ->
                    childNoteRepository.findByIdWithAuth(referenceId, receiverUserId).isPresent();
            case NOTE_COMMENT_ADDED, NOTE_REPLY_ADDED ->
                    noteCommentRepository.findByIdWithAuth(referenceId, receiverUserId).isPresent();
        };

        if (!authorized) {
            throw new AccessDeniedException("해당 알림에 대한 접근 권한이 없습니다.");
        }
    }

    public PageResponseDTO<NotificationDTO> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByReceiverUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponseDTO.from(page, NotificationDTO::from);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByNotificationIdAndReceiverUserId(notificationId, userId)
                .orElseThrow(() -> new AccessDeniedException("알림 조회 권한이 없거나 존재하지 않습니다."));
        notification.markAsRead();
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsReadByReceiverUserId(userId);
    }

    @Transactional
    public int markAllAsRead(UUID userId, NotificationType type) {
        if (type == null) {
            return markAllAsRead(userId);
        }
        return notificationRepository.markAllAsReadByReceiverUserIdAndType(userId, type);
    }

    @Transactional
    public int deleteOldNotifications(UUID userId, LocalDateTime before, NotificationType type) {
        if (before == null) {
            throw new IllegalArgumentException("before is required");
        }
        if (type == null) {
            return notificationRepository.deleteByReceiverUserIdAndCreatedAtBefore(userId, before);
        }
        return notificationRepository.deleteByReceiverUserIdAndTypeAndCreatedAtBefore(userId, type, before);
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
            emitters.remove(userId);
            emitter.complete();
        }
    }
}
