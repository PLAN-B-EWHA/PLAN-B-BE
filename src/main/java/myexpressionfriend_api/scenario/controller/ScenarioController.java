package myexpressionfriend_api.scenario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.scenario.dto.ScenarioBulkImportResultDTO;
import myexpressionfriend_api.scenario.dto.ScenarioDTO;
import myexpressionfriend_api.scenario.service.ScenarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Scenario", description = "시나리오 API")
public class ScenarioController {

    private final ScenarioService scenarioService;

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

    // ── Unity: 조회 (인증 불필요) ─────────────────────────────────────

    /**
     * 주차별 시나리오 목록 조회 (Unity 런타임용)
     * week 파라미터로 1~16주 중 원하는 주차를 지정합니다.
     */
    @GetMapping("/api/unity/scenarios")
    @Operation(summary = "주차별 시나리오 조회",
            description = "Unity 클라이언트가 지정 주차의 시나리오 전체를 가져옵니다. 인증 불필요.")
    public ResponseEntity<ApiResponse<List<ScenarioDTO>>> getScenariosForWeek(
            @RequestParam @Min(1) @Max(16) int week
    ) {
        List<ScenarioDTO> scenarios = scenarioService.getScenariosForWeek(week);
        return ResponseEntity.ok(ApiResponse.success(scenarios));
    }

    /**
     * 단건 시나리오 조회 (Unity 또는 관리자용)
     */
    @GetMapping("/api/unity/scenarios/{scenarioId}")
    @Operation(summary = "시나리오 단건 조회")
    public ResponseEntity<ApiResponse<ScenarioDTO>> getScenario(
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(ApiResponse.success(scenarioService.getScenario(scenarioId)));
    }
}
