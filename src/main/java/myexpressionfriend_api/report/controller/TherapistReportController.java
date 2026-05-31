package myexpressionfriend_api.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.report.domain.ReportStatus;
import myexpressionfriend_api.report.dto.ReportGenerateDraftRequest;
import myexpressionfriend_api.report.dto.ReportResponse;
import myexpressionfriend_api.report.dto.ReportUpdateRequest;
import myexpressionfriend_api.report.service.ReportService;
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
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('THERAPIST','ADMIN')")
@Tag(name = "치료사 - 리포트", description = "AI/RAG 기반 리포트 초안을 생성하고 치료사가 검토, 수정, 발행하는 API")
public class TherapistReportController {

    private final ReportService reportService;

    @PostMapping("/api/therapist/reports/drafts/generate")
    @Operation(summary = "리포트 초안 생성", description = "RAG로 관련 자료를 검색한 뒤 LLM으로 리포트 초안을 생성하고 DRAFT 상태로 저장합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> generateDraft(
            @Valid @RequestBody ReportGenerateDraftRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "리포트 초안이 생성되었습니다.",
                reportService.generateDraft(userId, request)));
    }

    @GetMapping("/api/therapist/children/{childId}/reports")
    @Operation(summary = "아동 리포트 목록 조회", description = "치료사가 특정 아동의 리포트 목록을 상태별로 조회합니다. 치료사용 응답에는 RAG 컨텍스트와 프롬프트 스냅샷이 포함됩니다.")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(
            @PathVariable UUID childId,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getReportsForTherapist(userId, childId, status, pageable)));
    }

    @GetMapping("/api/therapist/children/{childId}/reports/{reportId}")
    @Operation(summary = "아동 리포트 상세 조회", description = "치료사가 특정 리포트의 내용과 생성 근거 스냅샷을 조회합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable UUID childId,
            @PathVariable UUID reportId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getReportForTherapist(userId, childId, reportId)));
    }

    @PatchMapping("/api/therapist/children/{childId}/reports/{reportId}")
    @Operation(summary = "리포트 초안 수정", description = "발행 전 리포트의 제목과 본문을 수정합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(
            @PathVariable UUID childId,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportUpdateRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "리포트 초안이 수정되었습니다.",
                reportService.updateReport(userId, childId, reportId, request)));
    }

    @PatchMapping("/api/therapist/children/{childId}/reports/{reportId}/review")
    @Operation(summary = "리포트 검토 완료", description = "치료사가 리포트 내용을 검토하고 REVIEWED 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> reviewReport(
            @PathVariable UUID childId,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportUpdateRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "리포트 검토가 완료되었습니다.",
                reportService.reviewReport(userId, childId, reportId, request)));
    }

    @PatchMapping("/api/therapist/children/{childId}/reports/{reportId}/publish")
    @Operation(summary = "리포트 발행", description = "검토 완료된 리포트를 보호자가 조회할 수 있도록 PUBLISHED 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> publishReport(
            @PathVariable UUID childId,
            @PathVariable UUID reportId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "리포트가 발행되었습니다.",
                reportService.publishReport(userId, childId, reportId)));
    }

    @PatchMapping("/api/therapist/children/{childId}/reports/{reportId}/archive")
    @Operation(summary = "리포트 아카이브", description = "리포트를 ARCHIVED 상태로 변경합니다. 보호자에게 더 이상 노출되지 않습니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> archiveReport(
            @PathVariable UUID childId,
            @PathVariable UUID reportId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                "리포트가 아카이브되었습니다.",
                reportService.archiveReport(userId, childId, reportId)));
    }
}
