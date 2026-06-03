package myexpressionfriend_api.scenario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.PageResponseDTO;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.game.domain.ScenarioSource;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;
import myexpressionfriend_api.scenario.dto.AdminScenarioResponseDTO;
import myexpressionfriend_api.scenario.dto.ScenarioBulkImportResultDTO;
import myexpressionfriend_api.scenario.dto.ScenarioDTO;
import myexpressionfriend_api.scenario.dto.ScenarioReviewRequestDTO;
import myexpressionfriend_api.scenario.dto.ScenarioSeedBatchGenerateRequestDTO;
import myexpressionfriend_api.scenario.dto.ScenarioSeedBatchGenerateResponseDTO;
import myexpressionfriend_api.scenario.dto.ScenarioStatusResponseDTO;
import myexpressionfriend_api.scenario.dto.ScenarioStatusUpdateRequestDTO;
import myexpressionfriend_api.scenario.service.ScenarioSeedBatchGenerationService;
import myexpressionfriend_api.scenario.service.ScenarioService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "시나리오", description = "시나리오 등록, 생성, 검수, Unity 조회 API")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final ScenarioSeedBatchGenerationService scenarioSeedBatchGenerationService;

    // ── Admin: Import ─────────────────────────────────────────────────

    /**
     * 시나리오 일괄 저장 (Admin/Therapist 전용)
     * Body: 시나리오 DTO 배열 (기존 JSON 파일 형식 그대로)
     */
    @PostMapping("/api/admin/scenarios/bulk-import")
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(summary = "시나리오 일괄 Import",
            description = "JSON 파일 형식의 시나리오 배열을 DB에 저장합니다. 이미 존재하는 scenario_id는 건너뜁니다.")
    public ResponseEntity<ApiResponse<ScenarioBulkImportResultDTO>> bulkImport(
            @RequestBody List<ScenarioDTO> scenarios
    ) {
        ScenarioBulkImportResultDTO result = scenarioService.bulkImport(scenarios);
        return ResponseEntity.ok(ApiResponse.success(
                result.savedCount() + "개 시나리오가 저장되었습니다.", result));
    }

    @PostMapping(value = "/api/admin/scenarios/generate-from-seed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Seed CSV 기반 RAG 시나리오 배치 생성",
            description = "캐릭터와 범위를 지정해 seed CSV에서 시나리오를 생성하고 DB 저장 및 JSON 백업을 수행합니다.")
    public ResponseEntity<ApiResponse<ScenarioSeedBatchGenerateResponseDTO>> generateFromSeed(
            @Valid @RequestPart("request") ScenarioSeedBatchGenerateRequestDTO request,
            @RequestPart("file") MultipartFile seedCsv
    ) {
        ScenarioSeedBatchGenerateResponseDTO result = scenarioSeedBatchGenerationService.generate(request, seedCsv);
        return ResponseEntity.ok(ApiResponse.success("Seed 기반 시나리오 배치 생성이 완료되었습니다.", result));
    }

    @GetMapping("/api/admin/scenarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 시나리오 목록 조회", description = "DB에 저장된 시나리오를 상태, 출처, 주차, 키워드로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AdminScenarioResponseDTO>>> getAdminScenarios(
            @RequestParam(required = false) ScenarioApprovalStatus status,
            @RequestParam(required = false) ScenarioSource source,
            @RequestParam(required = false) @Min(1) @Max(16) Integer week,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponseDTO<AdminScenarioResponseDTO> result =
                scenarioService.searchAdminScenarios(status, source, week, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("시나리오 목록입니다.", result));
    }

    @GetMapping("/api/admin/scenarios/{scenarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 시나리오 상세 조회", description = "DB에 저장된 시나리오 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ScenarioDTO>> getAdminScenario(
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(ApiResponse.success(scenarioService.getScenario(scenarioId)));
    }

    @PatchMapping("/api/admin/scenarios/{scenarioId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 시나리오 상태 변경", description = "시나리오 상태를 DRAFT, PUBLISHED, REJECTED, ARCHIVED 중 하나로 변경합니다.")
    public ResponseEntity<ApiResponse<ScenarioStatusResponseDTO>> updateStatus(
            Authentication authentication,
            @PathVariable String scenarioId,
            @Valid @RequestBody ScenarioStatusUpdateRequestDTO request
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ScenarioStatusResponseDTO result = scenarioService.updateApprovalStatus(
                scenarioId, request.approvalStatus(), userId, request.reviewNote());
        return ResponseEntity.ok(ApiResponse.success("시나리오 상태가 변경되었습니다.", result));
    }

    @PostMapping("/api/admin/scenarios/{scenarioId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(summary = "시나리오 배포", description = "검수한 서버 시나리오를 Unity 조회 대상에 포함합니다.")
    public ResponseEntity<ApiResponse<ScenarioStatusResponseDTO>> publish(
            Authentication authentication,
            @PathVariable String scenarioId,
            @RequestBody(required = false) ScenarioReviewRequestDTO request
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ScenarioStatusResponseDTO result = scenarioService.publish(
                scenarioId, userId, request != null ? request.reviewNote() : null);
        return ResponseEntity.ok(ApiResponse.success("시나리오가 배포되었습니다.", result));
    }

    @PostMapping("/api/admin/scenarios/{scenarioId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(summary = "시나리오 반려", description = "검수한 서버 시나리오를 반려 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ScenarioStatusResponseDTO>> reject(
            Authentication authentication,
            @PathVariable String scenarioId,
            @RequestBody(required = false) ScenarioReviewRequestDTO request
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ScenarioStatusResponseDTO result = scenarioService.reject(
                scenarioId, userId, request != null ? request.reviewNote() : null);
        return ResponseEntity.ok(ApiResponse.success("시나리오가 반려되었습니다.", result));
    }

    @PostMapping("/api/admin/scenarios/{scenarioId}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(summary = "시나리오 보관", description = "배포 중이거나 검수된 서버 시나리오를 보관 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ScenarioStatusResponseDTO>> archive(
            Authentication authentication,
            @PathVariable String scenarioId,
            @RequestBody(required = false) ScenarioReviewRequestDTO request
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ScenarioStatusResponseDTO result = scenarioService.archive(
                scenarioId, userId, request != null ? request.reviewNote() : null);
        return ResponseEntity.ok(ApiResponse.success("시나리오가 보관되었습니다.", result));
    }

    // ── Unity: 조회 (인증 불필요) ─────────────────────────────────────

    /**
     * 주차별 시나리오 목록 조회 (Unity 런타임용)
     * week 파라미터로 1~16주 중 원하는 주차를 지정합니다.
     * 원본 시나리오 파일 구조 그대로 반환합니다 (ApiResponse 래퍼 없음).
     */
    @GetMapping(value = "/api/unity/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "주차별 시나리오 조회",
            description = "Unity 클라이언트가 지정 주차의 시나리오 전체를 가져옵니다. 인증 불필요. 원본 파일 JSON 구조 그대로 반환.")
    public ResponseEntity<List<ScenarioDTO>> getScenariosForWeek(
            @RequestParam @Min(1) @Max(16) int week
    ) {
        List<ScenarioDTO> scenarios = scenarioService.getScenariosForWeek(week);
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping(value = "/api/unity/scenarios/published", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "배포된 서버 시나리오 조회",
            description = "Unity 클라이언트가 웹에서 검수 및 배포된 서버 시나리오만 가져옵니다.")
    public ResponseEntity<List<ScenarioDTO>> getPublishedServerScenarios(
            Authentication authentication,
            @RequestParam(required = false) @Min(1) @Max(16) Integer week
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ScenarioDTO> scenarios = scenarioService.getPublishedServerScenarios(week, userId);
        return ResponseEntity.ok(scenarios);
    }

    /**
     * 단건 시나리오 조회 (Unity 또는 관리자용)
     * 원본 시나리오 파일 구조 그대로 반환합니다 (ApiResponse 래퍼 없음).
     */
    @GetMapping(value = "/api/unity/scenarios/{scenarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "시나리오 단건 조회",
            description = "원본 파일 JSON 구조 그대로 반환.")
    public ResponseEntity<ScenarioDTO> getScenario(
            Authentication authentication,
            @PathVariable String scenarioId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(scenarioService.getScenario(scenarioId, userId));
    }
}
