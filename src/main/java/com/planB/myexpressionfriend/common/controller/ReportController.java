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
import com.planB.myexpressionfriend.common.service.ReportGenerationService;
import com.planB.myexpressionfriend.common.service.ReportPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Slf4j
@Tag(name = "Report", description = "AI report preference and generated report API")
public class ReportController {

    private final ReportPreferenceService reportPreferenceService;
    private final GeneratedReportService generatedReportService;
    private final ReportGenerationService reportGenerationService;

    @GetMapping("/preferences/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my report preference")
    public ResponseEntity<ApiResponse<ReportPreferenceDTO>> getMyPreference(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        ReportPreference preference = reportPreferenceService.getOrCreate(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(ReportPreferenceDTO.from(preference)));
    }

    @PutMapping("/preferences/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my report preference")
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

        return ResponseEntity.ok(ApiResponse.success("Report preference updated", ReportPreferenceDTO.from(updated)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my generated reports")
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
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my generated report detail")
    public ResponseEntity<ApiResponse<GeneratedReportDTO>> getMyReportDetail(
            @PathVariable UUID reportId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        GeneratedReportDTO report = generatedReportService.getUserReportDTO(currentUser.getUserId(), reportId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/test-generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate report test", description = "Gemini를 호출해 리포트를 1회 수동 생성합니다.")
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
        return ResponseEntity.ok(ApiResponse.success("Report generated", report));
    }
}
