package com.planB.myexpressionfriend.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConstraintUpdater {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void updateNotificationTypeConstraint() {
        String sql = """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = current_schema()
                          AND table_name = 'notifications'
                    ) THEN
                        ALTER TABLE notifications
                            DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

                        ALTER TABLE notifications
                            ADD CONSTRAINT notifications_notification_type_check
                            CHECK (notification_type IN (
                                'MISSION_COMPLETED',
                                'MISSION_PHOTO_UPLOADED',
                                'REPORT_GENERATED',
                                'NOTE_COMMENT_ADDED',
                                'NOTE_REPLY_ADDED',
                                'NOTE_ASSET_UPLOADED'
                            ));
                    END IF;
                END $$;
                """;

        try {
            jdbcTemplate.execute(sql);
            log.info("Notification type check constraint is up to date.");
        } catch (Exception e) {
            log.warn("Failed to update notification type check constraint automatically.", e);
        }
    }
}

