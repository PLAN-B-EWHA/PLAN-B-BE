package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.child.AuthorizedUserDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildAuthorizationDTO;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.service.ChildAuthorizationService;
import com.planB.myexpressionfriend.common.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/authorizations")
@RequiredArgsConstructor
@Tag(name = "ChildAuthorization", description = "아동 권한 관리 API")
public class ChildAuthorizationController {

    private final ChildAuthorizationService authorizationService;

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 권한 부여", description = "주보호자가 특정 사용자에게 아동 접근 권한을 부여합니다.")
    public ResponseEntity<ApiResponse<AuthorizedUserDTO>> grantAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildAuthorizationDTO authorizationDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        AuthorizedUserDTO authorization = authorizationService.grantAuthorization(childId, userId, authorizationDTO);
        return ResponseEntity.ok(ApiResponse.success("권한이 부여되었습니다.", authorization));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동 권한 목록 조회", description = "특정 아동에 연결된 권한 보유 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<AuthorizedUserDTO>>> getAuthorizations(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<AuthorizedUserDTO> authorizations = authorizationService.getAuthorizedUsers(childId, userId);
        return ResponseEntity.ok(ApiResponse.success(authorizations));
    }

    @PutMapping("/{targetUserId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 권한 수정", description = "주보호자가 대상 사용자의 권한을 수정합니다.")
    public ResponseEntity<ApiResponse<AuthorizedUserDTO>> updateAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody ChildAuthorizationDTO authorizationDTO
    ) {
        UUID grantorUserId = SecurityContextUtil.getCurrentUserId(authentication);
        AuthorizedUserDTO authorization = authorizationService.updateAuthorization(
                childId, grantorUserId, targetUserId, authorizationDTO
        );
        return ResponseEntity.ok(ApiResponse.success("권한이 수정되었습니다.", authorization));
    }

    @DeleteMapping("/{targetUserId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 권한 회수", description = "주보호자가 대상 사용자의 아동 접근 권한을 회수합니다.")
    public ResponseEntity<ApiResponse<Void>> revokeAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID targetUserId
    ) {
        UUID grantorUserId = SecurityContextUtil.getCurrentUserId(authentication);
        authorizationService.revokeAuthorization(childId, grantorUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("권한이 회수되었습니다."));
    }
}