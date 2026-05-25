package myexpressionfriend_api.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.homework.domain.HomeworkStatus;
import myexpressionfriend_api.homework.dto.HomeworkAssignmentResponse;
import myexpressionfriend_api.homework.dto.HomeworkMissionSummaryResponse;
import myexpressionfriend_api.homework.dto.HomeworkReportSubmitRequest;
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
@RequestMapping("/api/parent/children/{childId}/homework")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "보호자 - 오프라인 미션", description = "보호자가 오프라인 미션을 조회하고 수행 기록을 제출하는 API")
public class ParentHomeworkController {

    private final HomeworkService homeworkService;

    @GetMapping
    @Operation(summary = "오프라인 미션 목록 조회", description = "보호자가 특정 아동에게 부여된 오프라인 미션 목록을 조회합니다.")
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
    @Operation(summary = "현재 오프라인 미션 조회", description = "보호자가 바로 수행하면 좋은 진행 전 오프라인 미션 1개를 조회합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> getCurrentAssignment(
            @PathVariable UUID childId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                homeworkService.getCurrentAssignment(userId, childId)));
    }

    @GetMapping("/summary")
    @Operation(summary = "오프라인 미션 요약 조회", description = "보호자 화면에서 사용할 제출률, 완료율, 자발성 비율 등 오프라인 미션 요약을 조회합니다.")
    public ResponseEntity<ApiResponse<HomeworkMissionSummaryResponse>> getMissionSummary(
            @PathVariable UUID childId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                homeworkService.getMissionSummary(userId, childId)));
    }

    @GetMapping("/{homeworkId}")
    @Operation(summary = "오프라인 미션 상세 조회", description = "보호자가 특정 오프라인 미션과 제출 기록을 조회합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> getAssignment(
            @PathVariable UUID childId,
            @PathVariable UUID homeworkId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                homeworkService.getAssignment(userId, childId, homeworkId)));
    }

    @PostMapping("/{homeworkId}/reports")
    @Operation(summary = "오프라인 미션 수행 기록 제출", description = "보호자가 가정에서 수행한 오프라인 미션 결과와 관찰 내용을 제출합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> submitReport(
            @PathVariable UUID childId,
            @PathVariable UUID homeworkId,
            @Valid @RequestBody HomeworkReportSubmitRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "숙제 리포트가 제출되었습니다.",
                homeworkService.submitReport(userId, childId, homeworkId, request)));
    }

    @PatchMapping("/{homeworkId}/reports")
    @Operation(summary = "오프라인 미션 수행 기록 수정", description = "치료사가 검토하기 전 제출 기록을 보호자가 수정합니다.")
    public ResponseEntity<ApiResponse<HomeworkAssignmentResponse>> updateReport(
            @PathVariable UUID childId,
            @PathVariable UUID homeworkId,
            @Valid @RequestBody HomeworkReportSubmitRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "오프라인 미션 기록이 수정되었습니다.",
                homeworkService.updateReport(userId, childId, homeworkId, request)));
    }
}
