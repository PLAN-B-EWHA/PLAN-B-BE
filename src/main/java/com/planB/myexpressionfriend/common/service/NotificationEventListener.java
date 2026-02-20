package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import com.planB.myexpressionfriend.common.event.MissionCompletedEvent;
import com.planB.myexpressionfriend.common.event.MissionPhotoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionCompleted(MissionCompletedEvent event) {
        if (event.therapistUserId().equals(event.parentUserId())) {
            return;
        }
        notificationService.saveAndSend(
                event.therapistUserId(),
                NotificationType.MISSION_COMPLETED,
                "미션 완료 알림",
                "부모가 홈 트레이닝 미션을 완료했습니다.",
                event.missionId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionPhotoUploaded(MissionPhotoUploadedEvent event) {
        if (event.therapistUserId().equals(event.uploaderUserId())) {
            return;
        }
        notificationService.saveAndSend(
                event.therapistUserId(),
                NotificationType.MISSION_PHOTO_UPLOADED,
                "미션 사진 등록 알림",
                "부모가 미션 증빙 사진을 업로드했습니다.",
                event.missionId()
        );
    }
}
