package com.planB.myexpressionfriend.common.controller;


import com.planB.myexpressionfriend.common.dto.ApiResponse;
import com.planB.myexpressionfriend.common.dto.UserDTO;
import com.planB.myexpressionfriend.common.dto.UserResponseDTO;
import com.planB.myexpressionfriend.common.dto.UserUpdateDTO;
import com.planB.myexpressionfriend.common.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMyInfo(
            @AuthenticationPrincipal UserDTO userDTO
    ) {
        log.info("============= 내 정보 조회 =============");
        log.info("사용자: {}", userDTO.getEmail());

        UserResponseDTO user = userService.getUserByEmail(userDTO.getEmail());

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 내 정보 수정
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateMyInfo(  // ← 수정!
                                                                       @AuthenticationPrincipal UserDTO userDTO,
                                                                       @Valid @RequestBody UserUpdateDTO updateDTO
    ) {
        log.info("============= 내 정보 수정 =============");
        log.info("사용자: {}", userDTO.getEmail());

        UserResponseDTO updatedUser = userService.updateUser(
                userDTO.getEmail(),
                updateDTO
        );

        return ResponseEntity.ok(
                ApiResponse.success("정보가 수정되었습니다", updatedUser)
        );
    }

    /**
     * 특정 사용자 조회 (관리자만)
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(  // ← 수정!
                                                                  @PathVariable UUID userId
    ) {
        log.info("============= 사용자 조회 (관리자) =============");
        log.info("대상 userId: {}", userId);

        UserResponseDTO user = userService.getUserById(userId);

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 전체 사용자 목록 조회 (관리자만)
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {  // ← 수정!
        log.info("============= 전체 사용자 조회 (관리자) =============");

        List<UserResponseDTO> users = userService.getAllUsers();

        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * 사용자 삭제 (관리자만)
     * DELETE /api/users/{userId}
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(  // ← 수정!
                                                          @PathVariable UUID userId
    ) {
        log.info("============= 사용자 삭제 (관리자) =============");
        log.info("대상 userId: {}", userId);

        userService.deleteUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success("사용자가 삭제되었습니다")
        );
    }
}
