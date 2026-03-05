package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.service.OpsMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ops")
@RequiredArgsConstructor
@Tag(name = "Ops", description = "운영 지표 API")
public class OpsController {

    private final OpsMetricService opsMetricService;

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "운영 지표 조회", description = "주요 실패/권한 거부 카운터를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.success(opsMetricService.snapshot()));
    }
}
