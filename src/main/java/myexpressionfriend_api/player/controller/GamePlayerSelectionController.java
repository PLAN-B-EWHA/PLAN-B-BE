package myexpressionfriend_api.player.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.player.dto.GamePlayerSelectionRequestDTO;
import myexpressionfriend_api.player.dto.GamePlayerSelectionResponseDTO;
import myexpressionfriend_api.player.service.GamePlayerSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/game-player")
@RequiredArgsConstructor
@Tag(name = "게임 플레이 아동 선택", description = "게임에서 사용할 플레이 대상 아동 선택 API")
public class GamePlayerSelectionController {

    private final GamePlayerSelectionService gamePlayerSelectionService;

    @PutMapping("/selected-child")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "플레이 아동 선택", description = "현재 사용자의 게임 플레이 대상 아동을 변경합니다.")
    public ResponseEntity<ApiResponse<GamePlayerSelectionResponseDTO>> setSelectedChild(
            Authentication authentication,
            @Valid @RequestBody GamePlayerSelectionRequestDTO requestDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        GamePlayerSelectionResponseDTO result = gamePlayerSelectionService.selectChild(userId, requestDTO.getChildId());
        return ResponseEntity.ok(ApiResponse.success("선택된 플레이 아동이 변경되었습니다.", result));
    }

    @GetMapping("/selected-child")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "플레이 아동 조회", description = "현재 사용자가 게임 플레이 대상으로 선택한 아동을 조회합니다.")
    public ResponseEntity<ApiResponse<GamePlayerSelectionResponseDTO>> getSelectedChild(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        GamePlayerSelectionResponseDTO result = gamePlayerSelectionService.getSelectedChild(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
