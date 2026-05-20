package myexpressionfriend_api.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.admin.dto.AdminUserRoleUpdateDTO;
import myexpressionfriend_api.admin.service.AdminUserService;
import myexpressionfriend_api.auth.dto.UserDTO;
import myexpressionfriend_api.auth.dto.UserResponseDTO;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 - 사용자 관리", description = "관리자 전용 사용자 관리 API")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "전체 사용자 조회", description = "모든 가입자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("전체 사용자 목록입니다.", users));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "특정 사용자 조회", description = "ID로 특정 사용자 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable UUID userId) {
        UserResponseDTO user = adminUserService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "사용자 권한 변경", description = "특정 사용자의 역할을 변경합니다. 자기 자신의 역할은 변경할 수 없습니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> changeUserRole(
            @AuthenticationPrincipal UserDTO adminUser,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserRoleUpdateDTO request
    ) {
        UserResponseDTO updated = adminUserService.changeUserRole(
                adminUser.getUserId(),
                userId,
                request.getRole()
        );
        return ResponseEntity.ok(ApiResponse.success("사용자 역할이 변경되었습니다.", updated));
    }
}
