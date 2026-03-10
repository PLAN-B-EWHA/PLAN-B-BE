package com.planB.myexpressionfriend.unity.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.unity.dto.UnityGameResultSaveRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityGameResultSaveResponseDTO;
import com.planB.myexpressionfriend.unity.service.UnityGameResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unity 게임 결과 저장 컨트롤러
 */
@RestController
@RequestMapping("/api/unity/game-results")
@RequiredArgsConstructor
@Tag(name = "UnityGameResult", description = "Unity 게임 결과 API")
public class UnityGameResultController {

    private final UnityGameResultService unityGameResultService;

    @PostMapping
    @Operation(summary = "Unity 게임 결과 저장", description = "Unity에서 전송한 게임 결과를 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<UnityGameResultSaveResponseDTO>> saveResult(
            @Valid @RequestBody UnityGameResultSaveRequestDTO requestDTO
    ) {
        UnityGameResultSaveResponseDTO result = unityGameResultService.saveResult(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Unity 게임 결과가 저장되었습니다.", result));
    }
}