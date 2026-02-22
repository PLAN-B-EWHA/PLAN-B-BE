package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.game.GameSessionDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.GameSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Tag(name = "GameSession", description = "게임 세션 API")
public class GameSessionController {

    private final GameSessionService sessionService;

    @GetMapping("/validate")
    @Operation(summary = "세션 검증", description = "게임 세션 토큰을 검증합니다.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> validateSession(
            @RequestParam String sessionToken
    ) {
        log.info("GET /api/game-sessions/validate");
        GameSessionDTO session = sessionService.validateSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PostMapping("/refresh")
    @Operation(summary = "세션 갱신", description = "게임 세션의 마지막 사용 시각을 갱신합니다.")
    public ResponseEntity<ApiResponse<Void>> refreshSession(
            @RequestParam String sessionToken
    ) {
        log.info("POST /api/game-sessions/refresh");
        sessionService.refreshSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success("Session refreshed"));
    }

    @GetMapping("/children/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동별 활성 세션 조회", description = "특정 아동의 활성 게임 세션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<GameSessionDTO>>> getActiveSessionsByChild(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/game-sessions/children/{} - userId={}", childId, currentUser.getUserId());

        List<GameSessionDTO> sessions = sessionService.getActiveSessionsByChild(
                childId, currentUser.getUserId()
        );

        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping
    @Operation(summary = "세션 종료", description = "세션 토큰으로 게임 세션을 종료합니다.")
    public ResponseEntity<ApiResponse<Void>> terminateSession(
            @RequestParam String sessionToken
    ) {
        log.info("DELETE /api/game-sessions");
        sessionService.terminateSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success("Session terminated"));
    }

    @DeleteMapping("/children/{childId}/all")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동의 모든 세션 종료", description = "특정 아동의 모든 활성 게임 세션을 종료합니다.")
    public ResponseEntity<ApiResponse<Void>> terminateAllSessionsByChild(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("DELETE /api/game-sessions/children/{}/all - userId={}", childId, currentUser.getUserId());

        sessionService.terminateAllSessionsByChild(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("All sessions terminated"));
    }
}
