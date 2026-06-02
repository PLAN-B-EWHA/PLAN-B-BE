package myexpressionfriend_api.realtime.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.realtime.dto.RealtimeAdminConfigResponse;
import myexpressionfriend_api.realtime.dto.RealtimeAdminConfigUpdateRequest;
import myexpressionfriend_api.realtime.service.OpenAiRealtimeAdminConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/realtime/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Realtime Config", description = "Admin endpoints for OpenAI Realtime runtime configuration")
public class AdminRealtimeConfigController {

    private final OpenAiRealtimeAdminConfigService configService;

    @GetMapping
    @Operation(summary = "Get OpenAI Realtime config")
    public ResponseEntity<ApiResponse<RealtimeAdminConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(configService.getConfig()));
    }

    @PatchMapping
    @Operation(summary = "Update OpenAI Realtime config")
    public ResponseEntity<ApiResponse<RealtimeAdminConfigResponse>> updateConfig(
            @Valid @RequestBody RealtimeAdminConfigUpdateRequest request
    ) {
        RealtimeAdminConfigResponse updated = configService.updateConfig(request);
        return ResponseEntity.ok(ApiResponse.success("Realtime config updated.", updated));
    }
}
