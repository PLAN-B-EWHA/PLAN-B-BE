package myexpressionfriend_api.common.controller;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.config.LlmProperties;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.service.LlmTextClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmHealthCheckController {

    private final LlmTextClient llmTextClient;
    private final LlmProperties llmProperties;

    @GetMapping("/health-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        String model = llmProperties.getModelFlash();
        String prompt = "Respond with exactly: OK";
        boolean ok = llmTextClient.generateText(model, prompt)
                .map(v -> v.toUpperCase().contains("OK"))
                .orElse(false);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "enabled", llmProperties.isEnabled(),
                "model", model,
                "ok", ok
        )));
    }
}
