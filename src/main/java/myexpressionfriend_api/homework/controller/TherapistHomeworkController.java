package myexpressionfriend_api.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.homework.domain.HomeworkStatus;
import myexpressionfriend_api.homework.dto.HomeworkAssignmentCreateRequest;
import myexpressionfriend_api.homework.dto.HomeworkAssignmentResponse;
import myexpressionfriend_api.homework.dto.HomeworkAssignmentUpdateRequest;
import myexpressionfriend_api.homework.dto.HomeworkGenerateMissionRequest;
import myexpressionfriend_api.homework.dto.HomeworkMissionSummaryResponse;
import myexpressionfriend_api.homework.dto.HomeworkReviewRequest;
import myexpressionfriend_api.homework.service.HomeworkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/therapist/children/{childId}/homework")
@RequiredArgsConstructor
@PreAuthorize("hasRole('THERAPIST')")
@Tag(name = "치료사 - 오프라인 미션", description = "치료사가 아동에게 오프라인 미션을 생성, 조회, 수정, 취소, 검토하는 API")
public class TherapistHomeworkController {

    private final HomeworkService homeworkService;

    @GetMapping
    @Operation(summary = "오프라인 미션 목록 조회", description = "특정 아동에게 부여된 오프라인 미션 목록을 상태별로 조회합니다.")
    public ResponseEntity<ApiResponse<Page<HomeworkAssignmentResponse>>> getAssignments(
            @PathVariable UUID childId,
            @RequestParam(required = false) HomeworkStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                homeworkService.getAssignments(userId, childId, status, pageable)));
    }

    @GetMapping("/current")
    @Operation(summary = "현재 오프라인 미션 조회", description = "해당 아동에게 남아 있는 진행 전 오프라인 미션 중 우선 확인할 1개를 조회합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> getCurrentAssignment(
            @PathVariable UUID childId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                homeworkService.getCurrentAssignment(userId, childId)));
    }

    @GetMapping("/summary")
    @Operation(summary = "오프라인 미션 요약 조회", description = "치료사용 오프라인 미션 제출률, 완료율, 자발성 비율, 주차별 요약을 조회합니다.")
    public ResponseEntity<ApiResponse<HomeworkMissionSummaryResponse>> getMissionSummary(
            @PathVariable UUID childId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                homeworkService.getMissionSummary(userId, childId)));
    }

    @GetMapping("/{homeworkId}")
    @Operation(summary = "오프라인 미션 상세 조회", description = "특정 오프라인 미션과 보호자 제출 기록을 조회합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> getAssignment(
            @PathVariable UUID childId,
            @PathVariable UUID homeworkId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                homeworkService.getAssignment(userId, childId, homeworkId)));
    }

    @PostMapping
    @Operation(summary = "오프라인 미션 직접 생성", description = "치료사가 직접 작성한 지시문과 전략 팁으로 오프라인 미션을 생성합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> createAssignment(
            @PathVariable UUID childId,
            @Valid @RequestBody HomeworkAssignmentCreateRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "Homework assignment created.",
                homeworkService.createAssignment(userId, childId, request)));
    }

    @PostMapping("/generate-offline-mission")
    @Operation(summary = "RAG 기반 오프라인 미션 생성", description = "치료 가이드라인과 아동 정보를 검색한 뒤 LLM으로 오프라인 미션 초안을 생성합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> generateOfflineMission(
            @PathVariable UUID childId,
            @Valid @RequestBody HomeworkGenerateMissionRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "RAG offline mission homework created.",
                homeworkService.generateOfflineMission(userId, childId, request)));
    }

    @PatchMapping("/{homeworkId}")
    @Operation(summary = "오프라인 미션 수정", description = "아직 제출되지 않은 오프라인 미션의 주차, 전략, 지시문, 팁, 마감일을 수정합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> updateAssignment(
            @PathVariable UUID childId,
            @PathVariable UUID homeworkId,
            @Valid @RequestBody HomeworkAssignmentUpdateRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "오프라인 미션이 수정되었습니다.",
                homeworkService.updateAssignment(userId, childId, homeworkId, request)));
    }

    @PatchMapping("/{homeworkId}/cancel")
    @Operation(summary = "오프라인 미션 취소", description = "검토 완료 전의 오프라인 미션을 취소 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> cancelAssignment(
            @PathVariable UUID childId,
            @PathVariable UUID homeworkId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "오프라인 미션이 취소되었습니다.",
                homeworkService.cancelAssignment(userId, childId, homeworkId)));
    }

    @PatchMapping("/{homeworkId}/review")
    @Operation(summary = "오프라인 미션 검토 완료", description = "보호자가 제출한 오프라인 미션 기록을 치료사가 검토하고 코멘트를 남깁니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> review(
            @PathVariable UUID childId,
            @PathVariable UUID homeworkId,
            @RequestBody(required = false) HomeworkReviewRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "오프라인 미션 검토가 완료되었습니다.",
                homeworkService.review(userId, childId, homeworkId, request)));
    }
}
