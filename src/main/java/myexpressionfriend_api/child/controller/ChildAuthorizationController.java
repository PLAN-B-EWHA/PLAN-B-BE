package myexpressionfriend_api.child.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.dto.AuthorizedUserResponseDTO;
import myexpressionfriend_api.child.dto.ChildAuthorizationCreateDTO;
import myexpressionfriend_api.child.dto.ChildAuthorizationUpdateDTO;
import myexpressionfriend_api.child.service.ChildAuthorizationService;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/authorizations")
@RequiredArgsConstructor
@Tag(name = "아동 권한", description = "아동 권한 관리 API")
public class ChildAuthorizationController {

    private final ChildAuthorizationService authorizationService;

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 권한 부여", description = "주보호자가 특정 사용자에게 아동 접근 권한을 부여합니다.")
    public ResponseEntity<ApiResponse<AuthorizedUserResponseDTO>> grantAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildAuthorizationCreateDTO createDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        AuthorizedUserResponseDTO result = authorizationService.grantAuthorization(
                childId, userId, createDTO
        );
        return ResponseEntity.ok(ApiResponse.success("권한이 부여되었습니다.", result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동 권한 목록 조회", description = "특정 아동에 연결된 권한 보유 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<AuthorizedUserResponseDTO>>> getAuthorizations(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<AuthorizedUserResponseDTO> result = authorizationService.getAuthorizedUsers(
                childId, userId
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{targetUserId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 권한 수정", description = "주보호자가 대상 사용자의 권한을 수정합니다.")
    public ResponseEntity<ApiResponse<AuthorizedUserResponseDTO>> updateAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody ChildAuthorizationUpdateDTO updateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        AuthorizedUserResponseDTO result = authorizationService.updateAuthorization(
                childId, userId, targetUserId, updateDTO
        );
        return ResponseEntity.ok(ApiResponse.success("권한이 수정되었습니다.", result));
    }

    @DeleteMapping("/{targetUserId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 권한 회수", description = "주보호자가 대상 사용자의 아동 접근 권한을 회수합니다.")
    public ResponseEntity<ApiResponse<Void>> revokeAuthorization(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID targetUserId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        authorizationService.revokeAuthorization(childId, userId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("권한이 회수되었습니다."));
    }
}
