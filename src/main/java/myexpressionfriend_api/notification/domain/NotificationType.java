package myexpressionfriend_api.notification.domain;

/**
 * 알림 유형
 */
public enum NotificationType {
    MISSION_COMPLETED,
    MISSION_PHOTO_UPLOADED,
    REPORT_GENERATED,
    NOTE_COMMENT_ADDED,
    NOTE_REPLY_ADDED,
    NOTE_ASSET_UPLOADED,
    /** 치료사 메모/코멘트 발행 시 보호자에게 전송 */
    COMMENT_ADDED,
    /** 주간 성장 요약 (매주 월요일) 보호자에게 전송 */
    WEEKLY_SUMMARY,
    /** 아동 N일 미접속 시 치료사에게 전송 */
    CHILD_INACTIVE
}
