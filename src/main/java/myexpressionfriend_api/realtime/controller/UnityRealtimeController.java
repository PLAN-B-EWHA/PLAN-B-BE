package myexpressionfriend_api.realtime.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.realtime.dto.RealtimeClientSecretResponse;
import myexpressionfriend_api.realtime.service.OpenAiRealtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/unity/realtime")
@RequiredArgsConstructor
@Tag(name = "Unity Realtime", description = "Unity client endpoints for OpenAI Realtime connection bootstrap")
public class UnityRealtimeController {

    private final OpenAiRealtimeService openAiRealtimeService;

    @PostMapping("/client-secret")
    @Operation(summary = "Create OpenAI Realtime client secret", description = "Creates a short-lived client secret after validating the logged-in user.")
    public ResponseEntity<ApiResponse<RealtimeClientSecretResponse>> createClientSecret(
            Authentication authentication
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        RealtimeClientSecretResponse result = openAiRealtimeService.createClientSecret();
        return ResponseEntity.ok(ApiResponse.success("Realtime client secret created for user " + userId, result));
    }
}
