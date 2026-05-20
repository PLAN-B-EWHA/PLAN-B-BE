package myexpressionfriend_api.notification.preference.repository;

import myexpressionfriend_api.notification.preference.domain.NotificationPreference;
import myexpressionfriend_api.notification.preference.domain.NotificationPreferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndPreferenceType(UUID userId, NotificationPreferenceType type);

    /** 스케줄러용: 특정 타입을 활성화한 사용자 목록 */
    List<NotificationPreference> findByPreferenceTypeAndEnabledTrue(NotificationPreferenceType type);
}
