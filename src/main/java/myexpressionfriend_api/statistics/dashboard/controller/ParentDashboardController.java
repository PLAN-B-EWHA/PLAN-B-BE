package myexpressionfriend_api.statistics.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.game.dto.history.DialogueHistoryDto;
import myexpressionfriend_api.game.dto.history.ExpressionHistoryDto;
import myexpressionfriend_api.statistics.dashboard.dto.DialogueProgressDto;
import myexpressionfriend_api.statistics.dashboard.dto.WeeklyHighlightDto;
import myexpressionfriend_api.statistics.dashboard.dto.WeeklyParticipationDto;
import myexpressionfriend_api.statistics.dashboard.service.ParentDashboardService;
import myexpressionfriend_api.statistics.dashboard.service.PlayHistoryService;
import myexpressionfriend_api.statistics.dialogue.dto.DialogueSummaryDto;
import myexpressionfriend_api.statistics.expression.dto.ExpressionSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "보호자 - 대시보드", description = "보호자가 아동의 학습 요약, 참여 현황, 게임 기록을 조회하는 API")
public class ParentDashboardController {

    private final ParentDashboardService parentDashboardService;
    private final PlayHistoryService playHistoryService;

    @GetMapping("/children/{childId}/expression/summary")
    @Operation(summary = "표정 학습 요약 조회", description = "아동의 표정 인식 학습 성공률, 유창성, 재시도 경향 등 요약 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<ExpressionSummaryDto>> getExpressionSummary(
            @PathVariable UUID childId,
            Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                parentDashboardService.getExpressionSummary(userId, childId)));
    }

    @GetMapping("/children/{childId}/dialogue/summary")
    @Operation(summary = "대화 학습 요약 조회", description = "아동의 대화 주제별 점수율, 라포 지수, 반복 패턴 등 요약 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<List<DialogueSummaryDto>>> getDialogueSummary(
            @PathVariable UUID childId,
            Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                parentDashboardService.getAllDialogueSummaries(userId, childId)));
    }

    @GetMapping("/children/{childId}/weekly-participation")
    @Operation(summary = "주간 참여 현황 조회", description = "이번 주 권장 학습일 대비 실제 참여일과 오프라인 미션 수행 현황을 조회합니다.")
    public ResponseEntity<ApiResponse<WeeklyParticipationDto>> getWeeklyParticipation(
            @PathVariable UUID childId,
            Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                parentDashboardService.getWeeklyParticipation(userId, childId)));
    }

    @GetMapping("/children/{childId}/weekly-highlight")
    @Operation(summary = "주간 하이라이트 조회", description = "이번 주 학습에서 보호자에게 보여줄 핵심 변화와 격려 메시지를 조회합니다.")
    public ResponseEntity<ApiResponse<WeeklyHighlightDto>> getWeeklyHighlight(
            @PathVariable UUID childId,
            Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                parentDashboardService.getWeeklyHighlight(userId, childId)));
    }

    @GetMapping("/children/{childId}/dialogue/progress")
    @Operation(summary = "대화 학습 진행도 조회", description = "대화 학습의 시간 흐름에 따른 점수와 참여 추이를 조회합니다.")
    public ResponseEntity<ApiResponse<DialogueProgressDto>> getDialogueProgress(
            @PathVariable UUID childId,
            Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                parentDashboardService.getDialogueProgress(userId, childId)));
    }

    @GetMapping("/children/{childId}/dialogue/history")
    @Operation(summary = "대화 게임 기록 조회", description = "아동의 대화 게임 세션 기록을 최신순으로 조회합니다. theme 값으로 주제를 필터링할 수 있습니다.")
    public ResponseEntity<ApiResponse<Page<DialogueHistoryDto>>> getDialogueHistory(
            @PathVariable UUID childId,
            @RequestParam(required = false) String theme,
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                playHistoryService.getDialogueHistory(userId, childId, theme, pageable)));
    }

    @GetMapping("/children/{childId}/expression/history")
    @Operation(summary = "표정 게임 기록 조회", description = "아동의 표정 게임 세션 기록을 최신순으로 조회합니다. emotion 값으로 감정 목표를 필터링할 수 있습니다.")
    public ResponseEntity<ApiResponse<Page<ExpressionHistoryDto>>> getExpressionHistory(
            @PathVariable UUID childId,
            @RequestParam(required = false) String emotion,
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                playHistoryService.getExpressionHistory(userId, childId, emotion, pageable)));
    }
}
