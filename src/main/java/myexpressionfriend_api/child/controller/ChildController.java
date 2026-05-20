package myexpressionfriend_api.child.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.dto.ChildCreateDTO;
import myexpressionfriend_api.child.dto.ChildDetailResponseDTO;
import myexpressionfriend_api.child.dto.ChildProfileUpdateDTO;
import myexpressionfriend_api.child.dto.ChildResponseDTO;
import myexpressionfriend_api.child.dto.ChildUpdateDTO;
import myexpressionfriend_api.child.dto.PinIssueResponseDTO;
import myexpressionfriend_api.child.dto.PinUpdateDTO;
import myexpressionfriend_api.child.dto.PinVerificationDTO;
import myexpressionfriend_api.child.dto.TransferPrimaryParentDTO;
import myexpressionfriend_api.child.service.ChildQueryService;
import myexpressionfriend_api.child.service.ChildService;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
@Tag(name = "아동", description = "아동 프로필과 접근 권한 관리 API")
public class ChildController {

    private final ChildService childService;
    private final ChildQueryService childQueryService;

    // ============= 학생 CRUD =============

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "학생 등록", description = "선택적 PIN과 함께 학생 프로필을 생성합니다.")
    public ResponseEntity<ApiResponse<ChildResponseDTO>> createChild(
            Authentication authentication,
            @Valid @RequestBody ChildCreateDTO createDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildResponseDTO child = childService.createChild(createDTO, userId);
        return ResponseEntity.ok(ApiResponse.success("아동이 생성되었습니다.", child));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "내 학생 목록 조회", description = "현재 사용자가 주보호자인 학생 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ChildResponseDTO>>> getMyChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildResponseDTO> children = childQueryService.getMyPrimaryChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/accessible")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "접근 가능한 학생 목록 조회", description = "현재 사용자가 접근 가능한 학생 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ChildResponseDTO>>> getAccessibleChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildResponseDTO> children = childQueryService.getAccessibleChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/playable")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "게임 가능한 학생 목록 조회", description = "PLAY_GAME 권한을 가진 학생 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ChildResponseDTO>>> getPlayableChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildResponseDTO> children = childQueryService.getPlayableChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "학생 상세 조회", description = "학생 상세 정보와 권한 보유 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<ChildDetailResponseDTO>> getChildDetail(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDetailResponseDTO child = childQueryService.getChildDetail(childId, userId);
        return ResponseEntity.ok(ApiResponse.success(child));
    }

    @PutMapping("/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "학생 정보 수정", description = "학생 프로필 전체 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<ChildResponseDTO>> updateChild(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildUpdateDTO updateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildResponseDTO child = childService.updateChild(childId, updateDTO, userId);
        return ResponseEntity.ok(ApiResponse.success("아동 정보가 수정되었습니다.", child));
    }

    @PatchMapping("/{childId}/profile")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "학생 프로필 부분 수정", description = "진단 정보를 제외한 프로필 필드를 부분 수정합니다.")
    public ResponseEntity<ApiResponse<ChildResponseDTO>> updateChildProfile(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildProfileUpdateDTO updateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildResponseDTO child = childService.updateChildProfile(childId, updateDTO, userId);
        return ResponseEntity.ok(ApiResponse.success("아동 프로필이 수정되었습니다.", child));
    }

    @DeleteMapping("/{childId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "학생 삭제", description = "주보호자만 학생을 소프트 삭제할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> deleteChild(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.deleteChild(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("아동이 삭제되었습니다."));
    }

    // ============= PIN 관리 =============

    @PutMapping("/{childId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "PIN 설정/변경", description = "새 PIN을 설정하거나 기존 PIN을 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> updatePin(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinUpdateDTO pinUpdateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.updatePin(childId, userId, pinUpdateDTO);
        return ResponseEntity.ok(ApiResponse.success("PIN이 변경되었습니다."));
    }    @PostMapping("/{childId}/pin/verify")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "PIN 검증", description = "학생의 PIN을 검증합니다.")
    public ResponseEntity<ApiResponse<Boolean>> verifyPin(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinVerificationDTO verificationDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        boolean isValid = childService.verifyPin(childId, userId, verificationDTO);
        return ResponseEntity.ok(ApiResponse.success(isValid));
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

    // ============= 프로필 이미지 =============

    @PostMapping(value = "/{childId}/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "프로필 이미지 업로드", description = "학생 프로필 이미지를 업로드합니다. MANAGE 권한 필요.")
    public ResponseEntity<ApiResponse<ChildResponseDTO>> uploadProfileImage(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildResponseDTO child = childService.uploadProfileImage(childId, userId, file);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 업로드되었습니다.", child));
    }

    @DeleteMapping("/{childId}/profile/image")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "프로필 이미지 삭제", description = "학생 프로필 이미지를 삭제합니다. MANAGE 권한 필요.")
    public ResponseEntity<ApiResponse<ChildResponseDTO>> deleteProfileImage(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildResponseDTO child = childService.deleteProfileImage(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 삭제되었습니다.", child));
    }

    // ============= 주보호자 이전 =============

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
