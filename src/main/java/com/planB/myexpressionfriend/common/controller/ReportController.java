package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.report.GeneratedReportDTO;
import com.planB.myexpressionfriend.common.dto.report.ReportGenerateTestRequestDTO;
import com.planB.myexpressionfriend.common.dto.report.ReportPreferenceDTO;
import com.planB.myexpressionfriend.common.dto.report.ReportPreferenceUpdateDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.GeneratedReportService;
import com.planB.myexpressionfriend.common.service.ReportExportService;
import com.planB.myexpressionfriend.common.service.ReportGenerationService;
import com.planB.myexpressionfriend.common.service.ReportPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "리포트 설정 및 생성 리포트 API")
public class ReportController {

    private final ReportPreferenceService reportPreferenceService;
    private final GeneratedReportService generatedReportService;
    private final ReportGenerationService reportGenerationService;
    private final ReportExportService reportExportService;

    @GetMapping("/preferences/me")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "내 리포트 설정 조회", description = "현재 사용자의 리포트 자동 발행 설정을 조회합니다.")
    public ResponseEntity<ApiResponse<ReportPreferenceDTO>> getMyPreference(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        ReportPreference preference = reportPreferenceService.getOrCreate(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(ReportPreferenceDTO.from(preference)));
    }

    @PutMapping("/preferences/me")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "내 리포트 설정 수정", description = "현재 사용자의 리포트 자동 발행 설정을 수정합니다.")
    public ResponseEntity<ApiResponse<ReportPreferenceDTO>> updateMyPreference(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser,
            @Valid @RequestBody ReportPreferenceUpdateDTO updateDTO
    ) {
        ReportPreference updated = reportPreferenceService.updatePreference(
                currentUser.getUserId(),
                updateDTO.getEnabled(),
                updateDTO.getScheduleType(),
                updateDTO.getDeliveryTime(),
                updateDTO.getTimezone(),
                updateDTO.getDeliveryChannel(),
                updateDTO.getChildScope(),
                updateDTO.getTargetChildId(),
                updateDTO.getLanguage(),
                updateDTO.getModelName(),
                updateDTO.getPromptTemplate(),
                updateDTO.getMaxTokens(),
                updateDTO.getAutoIssueOnNoData(),
                updateDTO.getCooldownHours()
        );
        return ResponseEntity.ok(ApiResponse.success("리포트 설정이 수정되었습니다.", ReportPreferenceDTO.from(updated)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "내 생성 리포트 목록 조회", description = "현재 사용자에게 발행된 리포트 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<GeneratedReportDTO>>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponseDTO<GeneratedReportDTO> reports =
                generatedReportService.getUserReportDTOs(currentUser.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "내 생성 리포트 상세 조회", description = "특정 리포트의 상세 내용을 조회합니다.")
    public ResponseEntity<ApiResponse<GeneratedReportDTO>> getMyReportDetail(
            @PathVariable UUID reportId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        GeneratedReportDTO report = generatedReportService.getUserReportDTO(currentUser.getUserId(), reportId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/{reportId}/export/csv")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "리포트 CSV 다운로드", description = "특정 리포트를 CSV 형식으로 다운로드합니다.")
    public ResponseEntity<byte[]> exportMyReportCsv(
            @PathVariable UUID reportId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        byte[] bytes = reportExportService.exportCsv(currentUser.getUserId(), reportId);
        String fileName = "report-" + reportId + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(bytes);
    }

    @GetMapping("/{reportId}/export/pdf")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "리포트 PDF 다운로드", description = "특정 리포트를 PDF 형식으로 다운로드합니다.")
    public ResponseEntity<byte[]> exportMyReportPdf(
            @PathVariable UUID reportId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        byte[] bytes = reportExportService.exportPdf(currentUser.getUserId(), reportId);
        String fileName = "report-" + reportId + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping("/test-generate")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "리포트 테스트 생성", description = "LLM 결과로 리포트를 1회 수동 생성합니다.")
    public ResponseEntity<ApiResponse<GeneratedReportDTO>> generateTestReport(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser,
            @Valid @RequestBody ReportGenerateTestRequestDTO requestDTO
    ) {
        GeneratedReportDTO report = reportGenerationService.generateTestReport(
                currentUser.getUserId(),
                requestDTO.getTargetChildId(),
                requestDTO.getPromptOverride(),
                requestDTO.getMaxTokens()
        );
        return ResponseEntity.ok(ApiResponse.success("리포트가 생성되었습니다.", report));
    }
}