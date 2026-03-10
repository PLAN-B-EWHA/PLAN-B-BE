package com.planB.myexpressionfriend.unity.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportResultDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionListResponseDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionResponseDTO;
import com.planB.myexpressionfriend.unity.service.UnityMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Unity 미션 가져오기 및 조회 컨트롤러
 */
@RestController
@RequestMapping("/api/unity/missions")
@RequiredArgsConstructor
@Tag(name = "UnityMission", description = "Unity 미션 연동 API")
public class UnityMissionController {

    private final UnityMissionService unityMissionService;

    @PostMapping("/import")
    @Operation(summary = "Unity 미션 가져오기", description = "Unity에서 보낸 미션 JSON 데이터를 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<UnityMissionImportResultDTO>> importMissions(
            @Valid @RequestBody UnityMissionImportRequestDTO requestDTO
    ) {
        UnityMissionImportResultDTO result = unityMissionService.importMissions(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Unity 미션이 저장되었습니다.", result));
    }

    @GetMapping("/latest")
    @Operation(summary = "최근 가져온 미션 조회", description = "검증용으로 최근 저장된 Unity 미션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<UnityMissionResponseDTO>>> getLatestMissions(
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<UnityMissionResponseDTO> result = unityMissionService.getLatestMissions(limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @Operation(summary = "Unity 런타임용 미션 조회", description = "Unity 실행 시 사용할 최신 미션 목록을 반환합니다.")
    public ResponseEntity<ApiResponse<UnityMissionListResponseDTO>> getMissionsForUnity() {
        List<UnityMissionResponseDTO> missions = unityMissionService.getMissionsForUnity();
        return ResponseEntity.ok(ApiResponse.success(UnityMissionListResponseDTO.builder()
                .missions(missions)
                .build()));
    }
}