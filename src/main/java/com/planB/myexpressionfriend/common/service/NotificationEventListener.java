package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import com.planB.myexpressionfriend.common.event.MissionCompletedEvent;
import com.planB.myexpressionfriend.common.event.MissionPhotoUploadedEvent;
import com.planB.myexpressionfriend.common.event.NoteAssetUploadedEvent;
import com.planB.myexpressionfriend.common.event.NoteCommentCreatedEvent;
import com.planB.myexpressionfriend.common.event.ReportGeneratedEvent;
import com.planB.myexpressionfriend.common.repository.ChildrenAuthorizedUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final ChildrenAuthorizedUserRepository authorizedUserRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionCompleted(MissionCompletedEvent event) {
        if (event.therapistUserId().equals(event.parentUserId())) {
            return;
        }
        notificationService.saveAndSend(
                event.therapistUserId(),
                NotificationType.MISSION_COMPLETED,
                "미션 완료 알림",
                "부모가 미션을 완료했습니다.",
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
                "부모가 미션 인증 사진을 업로드했습니다.",
                event.missionId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportGenerated(ReportGeneratedEvent event) {
        notificationService.saveAndSend(
                event.userId(),
                NotificationType.REPORT_GENERATED,
                "분석 리포트 생성 완료",
                "AI 분석 리포트를 생성했습니다.",
                event.reportId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNoteCommentCreated(NoteCommentCreatedEvent event) {
        String title = event.reply() ? "노트 답글 알림" : "노트 댓글 알림";
        String message = event.reply()
                ? "노트에 새로운 답글이 등록되었습니다."
                : "노트에 새로운 댓글이 등록되었습니다.";
        NotificationType type = event.reply()
                ? NotificationType.NOTE_REPLY_ADDED
                : NotificationType.NOTE_COMMENT_ADDED;

        authorizedUserRepository
                .findByChildIdAndPermission(event.childId(), ChildPermissionType.VIEW_REPORT)
                .stream()
                .map(au -> au.getUser().getUserId())
                .filter(userId -> !userId.equals(event.actorUserId()))
                .distinct()
                .forEach(userId -> notificationService.saveAndSend(
                        userId,
                        type,
                        title,
                        message,
                        event.commentId()
                ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNoteAssetUploaded(NoteAssetUploadedEvent event) {
        authorizedUserRepository
                .findByChildIdAndPermission(event.childId(), ChildPermissionType.VIEW_REPORT)
                .stream()
                .map(au -> au.getUser().getUserId())
                .filter(userId -> !userId.equals(event.actorUserId()))
                .distinct()
                .forEach(userId -> notificationService.saveAndSend(
                        userId,
                        NotificationType.NOTE_ASSET_UPLOADED,
                        "노트 첨부파일 등록 알림",
                        "노트에 새로운 첨부 이미지가 업로드되었습니다.",
                        event.noteId()
                ));
    }
}
