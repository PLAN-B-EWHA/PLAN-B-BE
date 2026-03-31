package myexpressionfriend_api.unity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.unity.dto.*;
import myexpressionfriend_api.unity.service.UnityMissionGenerationService;
import myexpressionfriend_api.unity.service.UnityMissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/unity/missions")
@RequiredArgsConstructor
@Tag(name = "UnityMission", description = "Unity 미션 연동 API")
public class UnityMissionController {

    private final UnityMissionService unityMissionService;
    private final UnityMissionGenerationService unityMissionGenerationService;

    @PostMapping("/import")
    @Operation(summary = "Unity 미션 가져오기", description = "Unity에서 전송한 미션 JSON 데이터를 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<UnityMissionImportResultDTO>> importMissions(
            @Valid @RequestBody UnityMissionImportRequestDTO requestDTO
    ) {
        UnityMissionImportResultDTO result = unityMissionService.importMissions(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Unity 미션이 저장되었습니다.", result));
    }

    @GetMapping("/latest")
    @Operation(summary = "최근 미션 조회", description = "검증용으로 최근 저장된 Unity 미션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<UnityMissionResponseDTO>>> getLatestMissions(
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<UnityMissionResponseDTO> result = unityMissionService.getLatestMissions(limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @Operation(summary = "Unity 런타임용 승인 미션 조회",
            description = "세션 토큰으로 해당 아동의 오늘 승인된 미션 목록을 반환합니다. 인증 불필요.")
    public ResponseEntity<ApiResponse<UnityMissionListResponseDTO>> getMissionsForUnity(
            @RequestParam String sessionToken
    ) {
        List<UnityMissionResponseDTO> missions = unityMissionService.getApprovedMissionsForUnity(sessionToken);
        return ResponseEntity.ok(ApiResponse.success(UnityMissionListResponseDTO.builder()
                .missions(missions)
                .build()));
    }

    @PatchMapping("/{missionId}/approve")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 승인", description = "PLAY_GAME 권한이 있는 보호자/치료사가 미션을 승인합니다.")
    public ResponseEntity<ApiResponse<UnityMissionResponseDTO>> approveMission(
            Authentication authentication,
            @PathVariable Long missionId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        UnityMissionResponseDTO result = unityMissionService.approveMission(missionId, userId);
        return ResponseEntity.ok(ApiResponse.success("미션이 승인되었습니다.", result));
    }

    @PatchMapping("/{missionId}/reject")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 거절", description = "PLAY_GAME 권한이 있는 보호자/치료사가 미션을 거절합니다.")
    public ResponseEntity<ApiResponse<UnityMissionResponseDTO>> rejectMission(
            Authentication authentication,
            @PathVariable Long missionId,
            @RequestParam(required = false) String reason
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        UnityMissionResponseDTO result = unityMissionService.rejectMission(missionId, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("미션이 거절되었습니다.", result));
    }

    @PostMapping("/generate/bulk")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Unity 미션 일괄 생성",
            description = "Expression/Situation 개수를 지정해 아동 맞춤 미션을 한 번에 생성하고 저장합니다.")
    public ResponseEntity<ApiResponse<UnityMissionImportResultDTO>> generateBulkMissions(
            Authentication authentication,
            @Valid @RequestBody UnityMissionBulkGenerateRequestDTO requestDTO
    ) {
        UUID userId = requireUserId(authentication);
        UnityMissionImportResultDTO result = unityMissionGenerationService.generateBulkMissionsForChild(
                requestDTO.getChildId(),
                userId,
                requestDTO.getExpressionCount(),
                requestDTO.getSituationCount(),
                requestDTO.getMaxTokens(),
                requestDTO.getModelName()
        );
        return ResponseEntity.ok(ApiResponse.success(result.getSavedCount() + "개 미션이 생성되었습니다.", result));
    }

    @PostMapping("/generate/preview")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Unity 미션 생성 미리보기",
            description = "아동 정보를 기반으로 LLM 미션을 생성하지만 저장하지 않습니다.")
    public ResponseEntity<ApiResponse<UnityMissionImportRequestDTO>> previewGeneratedMissions(
            Authentication authentication,
            @Valid @RequestBody UnityMissionGenerateRequestDTO requestDTO
    ) {
        UUID userId = requireUserId(authentication);
        UnityMissionImportRequestDTO result = unityMissionGenerationService.generateMissionRequestForChild(
                requestDTO.getChildId(),
                userId,
                requestDTO.getGenerationType(),
                requestDTO.getMissionIdStart(),
                requestDTO.getMaxTokens(),
                requestDTO.getModelName()
        );
        return ResponseEntity.ok(ApiResponse.success("Unity 미션 미리보기를 생성했습니다.", result));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Unity 미션 생성 및 저장",
            description = "아동 정보를 기반으로 LLM 미션을 생성하고 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<UnityMissionImportResultDTO>> generateAndSaveMissions(
            Authentication authentication,
            @Valid @RequestBody UnityMissionGenerateRequestDTO requestDTO
    ) {
        UUID userId = requireUserId(authentication);
        UnityMissionImportResultDTO result = unityMissionGenerationService.generateAndSaveMissionsForChild(
                requestDTO.getChildId(),
                userId,
                requestDTO.getGenerationType(),
                requestDTO.getMissionIdStart(),
                requestDTO.getMaxTokens(),
                requestDTO.getModelName()
        );
        return ResponseEntity.ok(ApiResponse.success("Unity 미션을 생성하고 저장했습니다.", result));
    }

    private UUID requireUserId(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        if (userId == null) {
            throw new InvalidRequestException("로그인 사용자 정보가 필요합니다.");
        }
        return userId;
    }
}
