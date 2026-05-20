package myexpressionfriend_api.notification.preference.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.notification.preference.domain.NotificationPreferenceType;
import myexpressionfriend_api.notification.preference.dto.NotificationPreferenceDto;
import myexpressionfriend_api.notification.preference.service.NotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "알림 설정", description = "사용자별 알림 수신 설정 조회 및 변경 API")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "알림 설정 목록 조회", description = "현재 로그인한 사용자의 알림 수신 설정 전체를 조회합니다.")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDto>>> getPreferences(
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(preferenceService.getPreferences(userId)));
    }

    @PutMapping("/{type}")
    @Operation(summary = "알림 설정 변경", description = "특정 알림 유형의 수신 여부와 추가 설정값을 변경합니다.")
    public ResponseEntity<ApiResponse<NotificationPreferenceDto>> updatePreference(
            @PathVariable NotificationPreferenceType type,
            @RequestBody UpdateRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                preferenceService.updatePreference(userId, type, request.enabled(), request.extraValue())));
    }

    record UpdateRequest(boolean enabled, Integer extraValue) {
    }
}
