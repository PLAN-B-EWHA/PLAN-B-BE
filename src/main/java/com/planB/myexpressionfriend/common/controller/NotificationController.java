package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.domain.notification.NotificationType;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.notification.NotificationDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "SSE 구독", description = "실시간 알림 스트림에 연결합니다.")
    public SseEmitter stream(@Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser) {
        return notificationService.subscribe(currentUser.getUserId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "알림 목록 조회", description = "전체 알림 목록을 페이지네이션으로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<NotificationDTO>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponseDTO<NotificationDTO> result = notificationService.getNotifications(currentUser.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        notificationService.markAsRead(notificationId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("읽음 처리했습니다."));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "알림 전체 읽음 처리", description = "옵션으로 특정 유형만 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @RequestParam(required = false) NotificationType type,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        int updatedCount = notificationService.markAllAsRead(currentUser.getUserId(), type);
        return ResponseEntity.ok(ApiResponse.success("전체 읽음 처리 완료", updatedCount));
    }

    @DeleteMapping("/cleanup")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "알림 기간 정리", description = "beforeDays 이전 알림을 옵션으로 유형별 삭제합니다.")
    public ResponseEntity<ApiResponse<Integer>> cleanupNotifications(
            @RequestParam(defaultValue = "30") int beforeDays,
            @RequestParam(required = false) NotificationType type,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        if (beforeDays <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("beforeDays must be positive", "INVALID_BEFORE_DAYS"));
        }
        LocalDateTime before = LocalDateTime.now().minusDays(beforeDays);
        int deletedCount = notificationService.deleteOldNotifications(currentUser.getUserId(), before, type);
        return ResponseEntity.ok(ApiResponse.success("정리 완료", deletedCount));
    }
}
