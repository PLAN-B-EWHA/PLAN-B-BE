package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.game.GameSessionDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.GameSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "게임 세션 생성", description = "아동의 게임 세션을 생성하고 세션 토큰을 반환합니다. Unity에 토큰을 전달하여 게임 결과와 아동을 연결합니다.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> createSession(
            @RequestParam UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        GameSessionDTO session = sessionService.createSession(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("게임 세션이 생성되었습니다.", session));
    }

    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "세션 검증", description = "게임 세션 토큰의 유효성을 검증합니다.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> validateSession(@RequestParam String sessionToken) {
        GameSessionDTO session = sessionService.validateSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "세션 갱신", description = "게임 세션의 마지막 사용 시각을 갱신합니다.")
    public ResponseEntity<ApiResponse<Void>> refreshSession(@RequestParam String sessionToken) {
        sessionService.refreshSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success("세션이 갱신되었습니다."));
    }

    @GetMapping("/children/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동별 활성 세션 조회", description = "특정 아동의 활성 게임 세션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<GameSessionDTO>>> getActiveSessionsByChild(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        List<GameSessionDTO> sessions = sessionService.getActiveSessionsByChild(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "세션 종료", description = "특정 게임 세션을 종료합니다.")
    public ResponseEntity<ApiResponse<Void>> terminateSession(
            @RequestParam String sessionToken,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        sessionService.terminateSession(sessionToken, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("세션이 종료되었습니다."));
    }

    @DeleteMapping("/children/{childId}/all")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동의 모든 세션 종료", description = "특정 아동의 활성 세션을 모두 종료합니다.")
    public ResponseEntity<ApiResponse<Void>> terminateAllSessionsByChild(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        sessionService.terminateAllSessionsByChild(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("모든 세션이 종료되었습니다."));
    }
}
