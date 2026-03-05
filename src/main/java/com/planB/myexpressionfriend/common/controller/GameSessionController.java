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
@Tag(name = "GameSession", description = "Game session APIs")
public class GameSessionController {

    private final GameSessionService sessionService;

    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Validate session", description = "Validate game session token.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> validateSession(@RequestParam String sessionToken) {
        GameSessionDTO session = sessionService.validateSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Refresh session", description = "Refresh last-used timestamp of session.")
    public ResponseEntity<ApiResponse<Void>> refreshSession(@RequestParam String sessionToken) {
        sessionService.refreshSession(sessionToken);
        return ResponseEntity.ok(ApiResponse.success("Session refreshed."));
    }

    @GetMapping("/children/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Get active sessions by child", description = "Get active game sessions for a child.")
    public ResponseEntity<ApiResponse<List<GameSessionDTO>>> getActiveSessionsByChild(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        List<GameSessionDTO> sessions = sessionService.getActiveSessionsByChild(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Terminate session", description = "Terminate a game session.")
    public ResponseEntity<ApiResponse<Void>> terminateSession(
            @RequestParam String sessionToken,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        sessionService.terminateSession(sessionToken, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Session terminated."));
    }

    @DeleteMapping("/children/{childId}/all")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Terminate all sessions of child", description = "Terminate all active sessions of a child.")
    public ResponseEntity<ApiResponse<Void>> terminateAllSessionsByChild(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        sessionService.terminateAllSessionsByChild(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("All sessions terminated."));
    }
}

