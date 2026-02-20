package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.child.AuthorizedUserDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildAuthorizationDTO;
import com.planB.myexpressionfriend.common.service.ChildAuthorizationService;
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
 * 아동 권한 관리 Controller
 */
@RestController
@RequestMapping("/api/children/{childId}/authorizations")
@RequiredArgsConstructor
@Slf4j
public class ChildAuthorizationController {

    private final ChildAuthorizationService authorizationService;

    /**
     * 권한 부여 (주보호자만)
     * POST /api/children/{childId}/authorizations
     */
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<AuthorizedUserDTO>> grantAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildAuthorizationDTO authorizationDTO
    ) {
        log.info("============= 권한 부여 =============");
        log.info("아동 ID: {}", childId);
        log.info("대상 사용자 ID: {}", authorizationDTO.getUserId());

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자 (주보호자): {}", userId);

        AuthorizedUserDTO authorization = authorizationService.grantAuthorization(
                childId, userId, authorizationDTO
        );

        return ResponseEntity.ok(
                ApiResponse.success("권한이 부여되었습니다", authorization)
        );
    }

    /**
     * 권한 목록 조회
     * GET /api/children/{childId}/authorizations
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<List<AuthorizedUserDTO>>> getAuthorizations(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        log.info("============= 권한 목록 조회 =============");
        log.info("아동 ID: {}", childId);

        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자: {}", userId);

        List<AuthorizedUserDTO> authorizations = authorizationService.getAuthorizedUsers(
                childId, userId
        );

        return ResponseEntity.ok(
                ApiResponse.success(authorizations)
        );
    }

    /**
     * 권한 수정 (주보호자만)
     * PUT /api/children/{childId}/authorizations/{userId}
     */
    @PutMapping("/{targetUserId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<AuthorizedUserDTO>> updateAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody ChildAuthorizationDTO authorizationDTO
    ) {
        log.info("============= 권한 수정 =============");
        log.info("아동 ID: {}", childId);
        log.info("대상 사용자 ID: {}", targetUserId);

        UUID grantorUserId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자 (주보호자): {}", grantorUserId);

        AuthorizedUserDTO authorization = authorizationService.updateAuthorization(
                childId, grantorUserId, targetUserId, authorizationDTO
        );

        return ResponseEntity.ok(
                ApiResponse.success("권한이 수정되었습니다", authorization)
        );
    }

    /**
     * 권한 취소 (주보호자만)
     * DELETE /api/children/{childId}/authorizations/{userId}
     */
    @DeleteMapping("/{targetUserId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> revokeAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID targetUserId
    ) {
        log.info("============= 권한 취소 =============");
        log.info("아동 ID: {}", childId);
        log.info("대상 사용자 ID: {}", targetUserId);

        UUID grantorUserId = SecurityContextUtil.getCurrentUserId(authentication);
        log.info("요청자 (주보호자): {}", grantorUserId);

        authorizationService.revokeAuthorization(childId, grantorUserId, targetUserId);

        return ResponseEntity.ok(
                ApiResponse.success("권한이 취소되었습니다")
        );
    }

}
