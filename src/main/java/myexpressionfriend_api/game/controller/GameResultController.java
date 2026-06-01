package myexpressionfriend_api.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.game.dto.DialogueResultSaveRequestDTO;
import myexpressionfriend_api.game.dto.ExpressionResultSaveRequestDTO;
import myexpressionfriend_api.game.service.GameResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/unity/game-results")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
@Tag(name = "게임 결과", description = "Unity 게임 결과 저장 API")
public class GameResultController {

    private final GameResultService gameResultService;

    @PostMapping("/dialogue")
    @Operation(summary = "대화 게임 결과 저장", description = "Unity에서 대화 세션 종료 후 결과를 전송합니다.")
    public ResponseEntity<ApiResponse<UUID>> saveDialogueResult(
            Authentication authentication,
            @Valid @RequestBody DialogueResultSaveRequestDTO dto
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info(
                "[game-result][dialogue] request received userId={}, scenarioId={}, theme={}, totalScore={}, maxScore={}, turns={}",
                userId,
                dto.scenarioId(),
                dto.theme(),
                dto.totalScore(),
                dto.maxScore(),
                dto.turns() == null ? 0 : dto.turns().size()
        );
        UUID sessionId = gameResultService.saveDialogueResult(userId, dto);
        log.info("[game-result][dialogue] response created userId={}, sessionId={}", userId, sessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("대화 게임 결과가 저장되었습니다.", sessionId));
    }

    @PostMapping("/expression")
    @Operation(summary = "표정 게임 결과 저장", description = "Unity에서 표정 세션 종료 후 결과를 전송합니다.")
    public ResponseEntity<ApiResponse<UUID>> saveExpressionResult(
            Authentication authentication,
            @Valid @RequestBody ExpressionResultSaveRequestDTO dto
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info(
                "[game-result][expression] request received userId={}, emotionTarget={}, finalAccuracy={}, isSuccess={}, tries={}",
                userId,
                dto.emotionTarget(),
                dto.finalAccuracy(),
                dto.isSuccess(),
                dto.tries() == null ? 0 : dto.tries().size()
        );
        UUID sessionId = gameResultService.saveExpressionResult(userId, dto);
        log.info("[game-result][expression] response created userId={}, sessionId={}", userId, sessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("표정 게임 결과가 저장되었습니다.", sessionId));
    }
}
