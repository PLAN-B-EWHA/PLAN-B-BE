package myexpressionfriend_api.notification.preference.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.notification.preference.domain.NotificationPreference;
import myexpressionfriend_api.notification.preference.domain.NotificationPreferenceType;
import myexpressionfriend_api.notification.preference.dto.NotificationPreferenceDto;
import myexpressionfriend_api.notification.preference.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private static final int DEFAULT_INACTIVE_DAYS = 7;

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * 현재 사용자의 모든 알림 설정 조회
     * 저장되지 않은 타입은 기본값으로 채워 반환
     */
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getPreferences(UUID userId) {
        Map<NotificationPreferenceType, NotificationPreference> saved = preferenceRepository
                .findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(NotificationPreference::getPreferenceType, p -> p));

        return Arrays.stream(NotificationPreferenceType.values())
                .map(type -> {
                    if (saved.containsKey(type)) {
                        NotificationPreference p = saved.get(type);
                        return new NotificationPreferenceDto(type, p.getEnabled(), p.getExtraValue());
                    }
                    return new NotificationPreferenceDto(type, true, defaultExtraValue(type));
                })
                .toList();
    }

    /**
     * 알림 설정 저장 (upsert)
     */
    @Transactional
    public NotificationPreferenceDto updatePreference(
            UUID userId, NotificationPreferenceType type, boolean enabled, Integer extraValue) {

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndPreferenceType(userId, type)
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId)
                        .preferenceType(type)
                        .build());

        pref.update(enabled, extraValue);
        preferenceRepository.save(pref);

        return new NotificationPreferenceDto(type, enabled, extraValue);
    }

    /**
     * 특정 알림 타입이 활성화되어 있는지 확인 (발송 전 체크용)
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId, NotificationPreferenceType type) {
        return preferenceRepository.findByUserIdAndPreferenceType(userId, type)
                .map(NotificationPreference::getEnabled)
                .orElse(true); // 설정 없으면 기본값 true
    }

    /**
     * extraValue 조회 (없으면 기본값 반환)
     */
    @Transactional(readOnly = true)
    public int getExtraValue(UUID userId, NotificationPreferenceType type) {
        return preferenceRepository.findByUserIdAndPreferenceType(userId, type)
                .map(p -> p.getExtraValue() != null ? p.getExtraValue() : defaultExtraValue(type))
                .orElse(defaultExtraValue(type));
    }

    private Integer defaultExtraValue(NotificationPreferenceType type) {
        return type == NotificationPreferenceType.CHILD_INACTIVE ? DEFAULT_INACTIVE_DAYS : null;
    }
}
