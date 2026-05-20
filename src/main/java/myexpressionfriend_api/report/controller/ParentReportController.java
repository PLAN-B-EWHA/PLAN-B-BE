package myexpressionfriend_api.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.report.dto.ReportResponse;
import myexpressionfriend_api.report.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/parent/children/{childId}/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "보호자 - 리포트", description = "보호자가 치료사가 발행한 리포트를 조회하는 API")
public class ParentReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "발행 리포트 목록 조회", description = "보호자가 특정 아동의 발행 완료된 리포트 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(
            @PathVariable UUID childId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getPublishedReportsForParent(userId, childId, pageable)));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "발행 리포트 상세 조회", description = "보호자가 발행 완료된 특정 리포트의 내용을 조회합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable UUID childId,
            @PathVariable UUID reportId,
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getPublishedReportForParent(userId, childId, reportId)));
    }
}
