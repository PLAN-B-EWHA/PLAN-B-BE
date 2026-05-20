package myexpressionfriend_api.notification.preference.dto;

import myexpressionfriend_api.notification.preference.domain.NotificationPreferenceType;

public record NotificationPreferenceDto(
        NotificationPreferenceType type,
        boolean enabled,
        Integer extraValue
) {}
