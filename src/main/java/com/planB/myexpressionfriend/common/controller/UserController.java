package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.dto.user.UserResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserRoleUpdateDTO;
import com.planB.myexpressionfriend.common.dto.user.UserUpdateDTO;
import com.planB.myexpressionfriend.common.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMyInfo(@AuthenticationPrincipal UserDTO userDTO) {
        UserResponseDTO user = userService.getUserByEmail(userDTO.getEmail());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me")
    @Operation(summary = "프로필 정보 수정", description = "현재 로그인한 사용자의 프로필 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateMyInfo(
            @AuthenticationPrincipal UserDTO userDTO,
            @Valid @RequestBody UserUpdateDTO updateDTO
    ) {
        UserResponseDTO updatedUser = userService.updateUser(userDTO.getEmail(), updateDTO);
        return ResponseEntity.ok(ApiResponse.success("프로필 정보가 수정되었습니다.", updatedUser));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 정보 조회", description = "관리자 권한으로 특정 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable UUID userId) {
        UserResponseDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "전체 사용자 조회", description = "관리자 권한으로 전체 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 역할 승급", description = "관리자 권한으로 PENDING 사용자를 지정한 역할로 승급합니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> promotePendingUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDTO currentUser,
            @Valid @RequestBody UserRoleUpdateDTO roleUpdateDTO
    ) {
        UserResponseDTO updatedUser = userService.promotePendingUser(
                currentUser.getUserId(),
                userId,
                roleUpdateDTO.getRole()
        );
        return ResponseEntity.ok(ApiResponse.success("사용자 역할이 승급되었습니다.", updatedUser));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 삭제", description = "관리자 권한으로 사용자를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자를 삭제했습니다."));
    }
}
