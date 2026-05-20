package myexpressionfriend_api.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.dto.common.PageResponseDTO;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.notification.dto.NotificationDTO;
import myexpressionfriend_api.notification.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림 목록 조회, 읽음 처리, 실시간 SSE 구독 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "실시간 알림 구독", description = "로그인한 사용자가 새 알림을 실시간으로 받을 수 있도록 SSE 스트림을 엽니다.")
    public SseEmitter subscribe(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return notificationService.subscribe(userId);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림 목록을 최신순으로 페이징 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<NotificationDTO>>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(userId, pageable)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "읽지 않은 알림 수 조회", description = "로그인한 사용자의 읽지 않은 알림 개수를 반환합니다.")
    public ResponseEntity<ApiResponse<Long>> countUnread(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnread(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable UUID notificationId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("알림이 읽음 처리되었습니다."));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "전체 알림 읽음 처리", description = "로그인한 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("전체 알림이 읽음 처리되었습니다.", count));
    }

    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "오래된 알림 삭제", description = "관리자가 지정한 일수 이전의 오래된 알림을 일괄 삭제합니다. 기본값은 90일입니다.")
    public ResponseEntity<ApiResponse<Integer>> cleanup(
            @RequestParam(defaultValue = "90") int daysAgo
    ) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysAgo);
        int deleted = notificationService.deleteOldNotifications(before);
        return ResponseEntity.ok(ApiResponse.success(deleted + "개의 알림을 삭제했습니다.", deleted));
    }
}
