package myexpressionfriend_api.statistics.dialogue.errorpattern.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.statistics.dialogue.errorpattern.service.DialogueErrorPatternBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/llm/error-pattern")
@RequiredArgsConstructor
@Tag(name = "관리자 - LLM 오류 패턴", description = "LLM으로 대화 게임의 0점 응답을 분석해 오류 패턴 통계를 갱신하는 관리자 API")
public class LlmErrorPatternAdminController {

    private final DialogueErrorPatternBatchService batchService;

    @PostMapping("/run")
    @Operation(summary = "LLM 오류 패턴 분석 실행", description = "특정 아동 또는 전체 아동의 대화 0점 응답을 LLM으로 분류하고 오류 패턴 요약 통계를 갱신합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> run(
            @RequestParam(required = false) UUID childId,
            @RequestParam(required = false) Integer maxTurns,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        if (childId != null) {
            batchService.refreshForChild(childId, true, maxTurns, force);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "mode", "single-child",
                    "childId", childId,
                    "maxTurns", maxTurns != null ? maxTurns : -1,
                    "force", force
            )));
        }

        batchService.runBiweeklyBatch();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "mode", "all-children"
        )));
    }

    @GetMapping("/run")
    @Operation(summary = "LLM 오류 패턴 분석 실행(GET)", description = "브라우저나 Swagger에서 간단히 호출할 수 있는 GET 방식의 오류 패턴 분석 실행 API입니다. POST /run과 동일하게 동작합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runGet(
            @RequestParam(required = false) UUID childId,
            @RequestParam(required = false) Integer maxTurns,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return run(childId, maxTurns, force);
    }
}
