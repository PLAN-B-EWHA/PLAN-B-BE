package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.child.*;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.game.GameSessionDTO;
import com.planB.myexpressionfriend.common.service.ChildService;
import com.planB.myexpressionfriend.common.service.GameSessionService;
import com.planB.myexpressionfriend.common.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 아동 관리 Controller
 */
@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
@Slf4j
public class ChildController {

    private final ChildService childService;
    private final GameSessionService gameSessionService;

    /**
     * 아동 생성 (PARENT만 가능)
     * POST /api/children
     */
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ChildDTO>> createChild(
            Authentication authentication,
            @Valid @RequestBody ChildCreateDTO createDTO
    ) {
        log.info("============= 아동 생성 =============");

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        ChildDTO child = childService.createChild(userId, createDTO);

        return ResponseEntity.ok(
                ApiResponse.success("아동이 생성되었습니다", child)
        );
    }

    /**
     * 내 아동 목록 조회 (주보호자)
     * GET /api/children/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getMyChildren(
            Authentication authentication
    ) {
        log.info("============= 내 아동 목록 조회 =============");

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        List<ChildDTO> children = childService.getMyChildren(userId);

        return ResponseEntity.ok(
                ApiResponse.success(children)
        );
    }

    /**
     * 접근 가능한 아동 목록 조회 (주보호자 + 권한 부여된 사용자)
     * GET /api/children/accessible
     */
    @GetMapping("/accessible")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getAccessibleChildren(
            Authentication authentication
    ) {
        log.info("============= 접근 가능한 아동 목록 조회 =============");

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        List<ChildDTO> children = childService.getAccessibleChildren(userId);

        return ResponseEntity.ok(
                ApiResponse.success(children)
        );
    }

    /**
     * Unity 플레이 가능한 아동 목록 조회
     * GET /api/children/playable
     */
    @GetMapping("/playable")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getPlayableChildren(
            Authentication authentication
    ) {
        log.info("============= 플레이 가능한 아동 목록 조회 =============");

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        List<ChildDTO> children = childService.getPlayableChildren(userId);

        return ResponseEntity.ok(
                ApiResponse.success(children)
        );
    }

    /**
     * 아동 상세 조회
     * GET /api/children/{childId}
     */
    @GetMapping("/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<ChildDetailDTO>> getChildDetail(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        log.info("============= 아동 상세 조회 =============");
        log.info("아동 ID: {}", childId);

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        ChildDetailDTO child = childService.getChildDetail(childId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(child)
        );
    }

    /**
     * 아동 정보 수정
     * PUT /api/children/{childId}
     */
    @PutMapping("/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<ChildDTO>> updateChild(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildUpdateDTO updateDTO
    ) {
        log.info("============= 아동 정보 수정 =============");
        log.info("아동 ID: {}", childId);

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        ChildDTO child = childService.updateChild(childId, userId, updateDTO);

        return ResponseEntity.ok(
                ApiResponse.success("아동 정보가 수정되었습니다", child)
        );
    }

    /**
     * 아동 삭제 (주보호자만, Soft Delete)
     * DELETE /api/children/{childId}
     */
    @DeleteMapping("/{childId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> deleteChild(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        log.info("============= 아동 삭제 =============");
        log.info("아동 ID: {}", childId);

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        childService.deleteChild(childId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("아동이 삭제되었습니다")
        );
    }

    /**
     * PIN 설정/변경 (주보호자만)
     * PUT /api/children/{childId}/pin
     */
    @PutMapping("/{childId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> updatePin(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinUpdateDTO pinUpdateDTO
    ) {
        log.info("============= PIN 설정/변경 =============");
        log.info("아동 ID: {}", childId);

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        childService.updatePin(childId, userId, pinUpdateDTO);

        return ResponseEntity.ok(
                ApiResponse.success("PIN이 설정되었습니다")
        );
    }

    /**
     * PIN 검증
     * POST /api/children/{childId}/pin/verify
     */
    @PostMapping("/{childId}/pin/verify")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<Boolean>> verifyPin(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinVerificationDTO verificationDTO
    ) {
        log.info("============= PIN 검증 =============");
        log.info("아동 ID: {}", childId);

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        boolean isValid = childService.verifyPin(childId, userId, verificationDTO);

        return ResponseEntity.ok(
                ApiResponse.success(isValid)
        );
    }

    /**
     * PIN 검증 + 게임 세션 생성 (유니티용)
     * POST /api/children/{childId}/pin/verify-and-start
     */
    @PostMapping("/{childId}/pin/verify-and-start")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")  // UserRole만 체크
    public ResponseEntity<ApiResponse<GameSessionDTO>> verifyPinAndStartGame(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinVerificationDTO verificationDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);

        // PIN 검증
        boolean isValid = childService.verifyPin(childId, userId, verificationDTO);
        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PIN이 일치하지 않습니다", "INVALID_PIN"));
        }

        // 게임 세션 생성 (내부에서 PLAY_GAME 권한 체크) ✅
        GameSessionDTO session = gameSessionService.createSession(childId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("게임 세션이 생성되었습니다", session)
        );
    }

    /**
     * PIN 제거 (주보호자만)
     * DELETE /api/children/{childId}/pin
     */
    @DeleteMapping("/{childId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> removePin(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestParam String currentPin
    ) {
        log.info("============= PIN 제거 =============");
        log.info("아동 ID: {}", childId);

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        childService.removePin(childId, userId, currentPin);

        return ResponseEntity.ok(
                ApiResponse.success("PIN이 제거되었습니다")
        );
    }

    /**
     * 주보호자 변경 (양육권 이전)
     * POST /api/children/{childId}/transfer-primary
     */
    @PostMapping("/{childId}/transfer-primary")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> transferPrimaryParent(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody TransferPrimaryParentDTO transferDTO
    ) {
        log.info("============= 주보호자 변경 =============");
        log.info("아동 ID: {}", childId);
        log.info("새 주보호자 ID: {}", transferDTO.getNewPrimaryParentUserId());

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("현재 주보호자: {}", userId);

        childService.transferPrimaryParent(childId, userId, transferDTO);

        return ResponseEntity.ok(
                ApiResponse.success("주보호자가 변경되었습니다")
        );
    }

}
