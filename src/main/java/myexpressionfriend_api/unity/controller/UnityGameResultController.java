package myexpressionfriend_api.unity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.unity.dto.UnityGameResultSaveRequestDTO;
import myexpressionfriend_api.unity.dto.UnityGameResultSaveResponseDTO;
import myexpressionfriend_api.unity.service.UnityGameResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unity 게임 결과 저장 컨트롤러
 * - 인증 불필요 (세션 토큰으로 아동 식별)
 */
@RestController
@RequestMapping("/api/unity/game-results")
@RequiredArgsConstructor
@Tag(name = "UnityGameResult", description = "Unity 게임 결과 API")
public class UnityGameResultController {

    private final UnityGameResultService unityGameResultService;

    @PostMapping
    @Operation(summary = "Unity 게임 결과 저장",
            description = "Unity에서 전송한 게임 결과를 저장합니다. 세션 토큰으로 아동을 식별합니다.")
    public ResponseEntity<ApiResponse<UnityGameResultSaveResponseDTO>> saveResult(
            @Valid @RequestBody UnityGameResultSaveRequestDTO requestDTO
    ) {
        UnityGameResultSaveResponseDTO result = unityGameResultService.saveResult(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Unity 게임 결과가 저장되었습니다.", result));
    }
}
