package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.child.ChildCreateDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildDetailDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildProfileUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.PinIssueResponseDTO;
import com.planB.myexpressionfriend.common.dto.child.PinUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.PinVerificationDTO;
import com.planB.myexpressionfriend.common.dto.child.TransferPrimaryParentDTO;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.game.GameSessionDTO;
import com.planB.myexpressionfriend.common.service.ChildService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
@Tag(name = "Child", description = "아동 관리 API")
public class ChildController {

    private final ChildService childService;

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 생성", description = "선택적 PIN과 함께 아동 프로필을 생성합니다.")
    public ResponseEntity<ApiResponse<ChildDTO>> createChild(
            Authentication authentication,
            @Valid @RequestBody ChildCreateDTO createDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.createChild(userId, createDTO);
        return ResponseEntity.ok(ApiResponse.success("아동이 생성되었습니다.", child));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "내 아동 목록 조회", description = "현재 사용자가 주보호자인 아동 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getMyChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildDTO> children = childService.getMyChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/accessible")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "접근 가능한 아동 목록 조회", description = "현재 사용자가 접근 가능한 아동 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getAccessibleChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildDTO> children = childService.getAccessibleChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/playable")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "게임 가능한 아동 목록 조회", description = "현재 사용자가 PLAY_GAME 권한을 가진 아동 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getPlayableChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildDTO> children = childService.getPlayableChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동 상세 조회", description = "아동 상세 정보와 권한 보유 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<ChildDetailDTO>> getChildDetail(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDetailDTO child = childService.getChildDetail(childId, userId);
        return ResponseEntity.ok(ApiResponse.success(child));
    }

    @PutMapping("/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동 정보 전체 수정", description = "아동 프로필 정보를 전체 수정합니다.")
    public ResponseEntity<ApiResponse<ChildDTO>> updateChild(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildUpdateDTO updateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.updateChild(childId, userId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("아동 정보가 수정되었습니다.", child));
    }

    @PatchMapping("/{childId}/profile")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동 프로필 일부 수정", description = "프로필 관련 필드만 부분 수정합니다.")
    public ResponseEntity<ApiResponse<ChildDTO>> updateChildProfile(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildProfileUpdateDTO updateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.updateChildProfile(childId, userId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("아동 프로필이 수정되었습니다.", child));
    }

    @PostMapping("/{childId}/profile-image")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "프로필 이미지 업로드", description = "아동 프로필 이미지를 업로드합니다.")
    public ResponseEntity<ApiResponse<ChildDTO>> uploadProfileImage(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.uploadProfileImage(childId, userId, file);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 업로드되었습니다.", child));
    }

    @DeleteMapping("/{childId}/profile-image")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "프로필 이미지 삭제", description = "아동 프로필 이미지를 삭제합니다.")
    public ResponseEntity<ApiResponse<ChildDTO>> deleteProfileImage(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.deleteProfileImage(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 삭제되었습니다.", child));
    }

    @DeleteMapping("/{childId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "아동 삭제", description = "주보호자만 아동을 소프트 삭제할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> deleteChild(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.deleteChild(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("아동이 삭제되었습니다."));
    }

    @PutMapping("/{childId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "PIN 설정 또는 변경", description = "새 PIN을 설정하거나 기존 PIN을 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> updatePin(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinUpdateDTO pinUpdateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.updatePin(childId, userId, pinUpdateDTO);
        return ResponseEntity.ok(ApiResponse.success("PIN이 변경되었습니다."));
    }

    @PostMapping("/{childId}/pin/issue-temp")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "임시 PIN 발급", description = "한 번만 확인 가능한 임시 PIN을 발급합니다.")
    public ResponseEntity<ApiResponse<PinIssueResponseDTO>> issueTemporaryPin(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        String pin = childService.issueTemporaryPin(childId, userId);
        return ResponseEntity.ok(ApiResponse.success(
                "임시 PIN이 발급되었습니다.",
                PinIssueResponseDTO.builder().pin(pin).build()
        ));
    }

    @PostMapping("/{childId}/pin/verify")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "PIN 검증", description = "아동의 PIN을 검증합니다.")
    public ResponseEntity<ApiResponse<Boolean>> verifyPin(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinVerificationDTO verificationDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        boolean isValid = childService.verifyPin(childId, userId, verificationDTO);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    @PostMapping("/{childId}/pin/verify-and-start")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "PIN 검증 후 게임 시작", description = "PIN 검증 후 게임 세션 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> verifyPinAndStartGame(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestBody PinVerificationDTO verificationDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        GameSessionDTO session = childService.verifyPinAndCreateSession(childId, userId, verificationDTO);
        return ResponseEntity.ok(ApiResponse.success("게임 세션이 생성되었습니다.", session));
    }

    @DeleteMapping("/{childId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "PIN 제거", description = "현재 PIN 검증 후 PIN을 제거합니다.")
    public ResponseEntity<ApiResponse<Void>> removePin(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestParam String currentPin
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.removePin(childId, userId, currentPin);
        return ResponseEntity.ok(ApiResponse.success("PIN이 제거되었습니다."));
    }

    @PostMapping("/{childId}/transfer-primary")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "주보호자 권한 이전", description = "다른 부모 사용자에게 주보호자 권한을 이전합니다.")
    public ResponseEntity<ApiResponse<Void>> transferPrimaryParent(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody TransferPrimaryParentDTO transferDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.transferPrimaryParent(childId, userId, transferDTO);
        return ResponseEntity.ok(ApiResponse.success("주보호자 권한이 이전되었습니다."));
    }
}