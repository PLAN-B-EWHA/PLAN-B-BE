package myexpressionfriend_api.notification.preference.domain;

public enum NotificationPreferenceType {
    COMMENT_ADDED,   // 부모: 치료사 메모 발행 시
    WEEKLY_SUMMARY,  // 부모: 주간 성장 요약 (매주 월요일)
    CHILD_INACTIVE   // 치료사: 아동 N일 미접속 (기본 7일)
}
