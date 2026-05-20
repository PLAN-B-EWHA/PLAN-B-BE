package myexpressionfriend_api.notification.preference.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_pref_user_type",
                columnNames = {"user_id", "preference_type"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 30)
    private NotificationPreferenceType preferenceType;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /** 추가 설정값 (CHILD_INACTIVE: 미접속 기준 일수) */
    @Column(name = "extra_value")
    private Integer extraValue;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(boolean enabled, Integer extraValue) {
        this.enabled = enabled;
        this.extraValue = extraValue;
    }
}
