package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.notification.Notification;
import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import com.planB.myexpressionfriend.common.repository.AssignedMissionRepository;
import com.planB.myexpressionfriend.common.repository.GeneratedReportRepository;
import com.planB.myexpressionfriend.common.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceAuthorizationTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AssignedMissionRepository assignedMissionRepository;
    @Mock
    private GeneratedReportRepository generatedReportRepository;
    @Mock
    private OpsMetricService opsMetricService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("권한 없는 사용자는 미션 알림 저장이 거부된다")
    void saveAndSend_missionUnauthorized_throwsAccessDenied() {
        UUID receiverUserId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();

        when(assignedMissionRepository.findByIdWithAuth(missionId, receiverUserId))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                notificationService.saveAndSend(
                        receiverUserId,
                        NotificationType.MISSION_COMPLETED,
                        "title",
                        "message",
                        missionId
                ));

        verify(notificationRepository, never()).saveAndFlush(any(Notification.class));
    }

    @Test
    @DisplayName("권한 없는 사용자는 리포트 알림 저장이 거부된다")
    void saveAndSend_reportUnauthorized_throwsAccessDenied() {
        UUID receiverUserId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        when(generatedReportRepository.findAuthorizedByReportId(reportId, receiverUserId))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                notificationService.saveAndSend(
                        receiverUserId,
                        NotificationType.REPORT_GENERATED,
                        "title",
                        "message",
                        reportId
                ));

        verify(notificationRepository, never()).saveAndFlush(any(Notification.class));
    }

    @Test
    @DisplayName("권한 있는 사용자는 미션 알림 저장이 허용된다")
    void saveAndSend_missionAuthorized_saves() {
        UUID receiverUserId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();

        Notification saved = Notification.builder()
                .notificationId(UUID.randomUUID())
                .receiverUserId(receiverUserId)
                .type(NotificationType.MISSION_COMPLETED)
                .title("title")
                .message("message")
                .referenceId(missionId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(assignedMissionRepository.findByIdWithAuth(missionId, receiverUserId))
                .thenReturn(Optional.of(mock(com.planB.myexpressionfriend.common.domain.mission.AssignedMission.class)));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(saved);

        notificationService.saveAndSend(
                receiverUserId,
                NotificationType.MISSION_COMPLETED,
                "title",
                "message",
                missionId
        );

        verify(notificationRepository).saveAndFlush(any(Notification.class));
    }
}
