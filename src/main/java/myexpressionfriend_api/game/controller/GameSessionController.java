package myexpressionfriend_api.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.game.dto.GameSessionDTO;
import myexpressionfriend_api.game.service.GameSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/game-sessions")
@RequiredArgsConstructor
@Tag(name = "GameSession", description = "게임 세션 API")
public class GameSessionController {

    private final GameSessionService sessionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "게임 세션 생성",
            description = "학생의 게임 세션을 생성하고 세션 토큰을 반환합니다. Unity에 토큰을 전달하여 학생과 게임 결과를 연결합니다.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> createSession(
            Authentication authentication,
            @RequestParam UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        GameSessionDTO session = sessionService.createSession(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("게임 세션이 생성되었습니다.", session));
    }

    @GetMapping("/validate")
    @Operation(summary = "세션 검증", description = "게임 세션 토큰의 유효성을 검증합니다.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> validateSession(@RequestParam String sessionToken) {
        GameSessionDTO session = sessionService.validateSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PostMapping("/refresh")
    @Operation(summary = "세션 갱신", description = "게임 세션의 마지막 사용 시각을 갱신합니다.")
    public ResponseEntity<ApiResponse<Void>> refreshSession(@RequestParam String sessionToken) {
        sessionService.refreshSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success("세션이 갱신되었습니다."));
    }

    @GetMapping("/children/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "학생별 활성 세션 조회", description = "특정 학생의 활성 게임 세션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<GameSessionDTO>>> getActiveSessionsByChild(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<GameSessionDTO> sessions = sessionService.getActiveSessionsByChild(childId, userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "세션 종료", description = "특정 게임 세션을 종료합니다.")
    public ResponseEntity<ApiResponse<Void>> terminateSession(
            Authentication authentication,
            @RequestParam String sessionToken
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        sessionService.terminateSession(sessionToken, userId);
        return ResponseEntity.ok(ApiResponse.success("세션이 종료되었습니다."));
    }

    @DeleteMapping("/children/{childId}/all")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "학생의 모든 세션 종료", description = "특정 학생의 활성 세션을 모두 종료합니다.")
    public ResponseEntity<ApiResponse<Void>> terminateAllSessionsByChild(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        sessionService.terminateAllSessionsByChild(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("모든 세션이 종료되었습니다."));
    }
}
