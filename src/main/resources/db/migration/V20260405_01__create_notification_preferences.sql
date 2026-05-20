-- notification_preferences 테이블 생성
-- 사용자별 알림 수신 설정 (COMMENT_ADDED / WEEKLY_SUMMARY / CHILD_INACTIVE)

CREATE TABLE IF NOT EXISTS notification_preferences (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL,
    preference_type  VARCHAR(30)  NOT NULL,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    extra_value      INTEGER,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notification_preferences PRIMARY KEY (id),
    CONSTRAINT uk_notification_pref_user_type UNIQUE (user_id, preference_type)
);

CREATE INDEX IF NOT EXISTS idx_notif_pref_user_id
    ON notification_preferences (user_id);

CREATE INDEX IF NOT EXISTS idx_notif_pref_type_enabled
    ON notification_preferences (preference_type, enabled);

COMMENT ON TABLE notification_preferences IS '사용자별 알림 수신 설정';
COMMENT ON COLUMN notification_preferences.user_id IS '설정 소유 사용자 ID';
COMMENT ON COLUMN notification_preferences.preference_type IS 'COMMENT_ADDED | WEEKLY_SUMMARY | CHILD_INACTIVE';
COMMENT ON COLUMN notification_preferences.enabled IS '알림 활성화 여부';
COMMENT ON COLUMN notification_preferences.extra_value IS '추가 설정값 (CHILD_INACTIVE: 미접속 기준 일수, 기본 7)';
COMMENT ON COLUMN notification_preferences.updated_at IS '마지막 설정 변경 시각';
