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
@Tag(name = "Child", description = "Child management APIs")
public class ChildController {

    private final ChildService childService;

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Create child", description = "Create a child profile with optional PIN.")
    public ResponseEntity<ApiResponse<ChildDTO>> createChild(
            Authentication authentication,
            @Valid @RequestBody ChildCreateDTO createDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.createChild(userId, createDTO);
        return ResponseEntity.ok(ApiResponse.success("Child created.", child));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get my children", description = "Get children where current parent is primary.")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getMyChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildDTO> children = childService.getMyChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/accessible")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Get accessible children", description = "Get children accessible by current user.")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getAccessibleChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildDTO> children = childService.getAccessibleChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/playable")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Get playable children", description = "Get children where user has PLAY_GAME permission.")
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getPlayableChildren(Authentication authentication) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        List<ChildDTO> children = childService.getPlayableChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{childId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Get child detail", description = "Get child details and authorized users.")
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
    @Operation(summary = "Update child", description = "Full update for child profile.")
    public ResponseEntity<ApiResponse<ChildDTO>> updateChild(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildUpdateDTO updateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.updateChild(childId, userId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("Child updated.", child));
    }

    @PatchMapping("/{childId}/profile")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Patch child profile", description = "Partial update for profile fields.")
    public ResponseEntity<ApiResponse<ChildDTO>> updateChildProfile(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildProfileUpdateDTO updateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.updateChildProfile(childId, userId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("Child profile updated.", child));
    }

    @PostMapping("/{childId}/profile-image")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Upload profile image", description = "Upload child profile image.")
    public ResponseEntity<ApiResponse<ChildDTO>> uploadProfileImage(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.uploadProfileImage(childId, userId, file);
        return ResponseEntity.ok(ApiResponse.success("Profile image uploaded.", child));
    }

    @DeleteMapping("/{childId}/profile-image")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Delete profile image", description = "Delete child profile image.")
    public ResponseEntity<ApiResponse<ChildDTO>> deleteProfileImage(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        ChildDTO child = childService.deleteProfileImage(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("Profile image deleted.", child));
    }

    @DeleteMapping("/{childId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Delete child", description = "Delete child (soft delete, primary parent only).")
    public ResponseEntity<ApiResponse<Void>> deleteChild(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.deleteChild(childId, userId);
        return ResponseEntity.ok(ApiResponse.success("Child deleted."));
    }

    @PutMapping("/{childId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Set or change PIN", description = "Set new PIN or change existing PIN.")
    public ResponseEntity<ApiResponse<Void>> updatePin(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinUpdateDTO pinUpdateDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.updatePin(childId, userId, pinUpdateDTO);
        return ResponseEntity.ok(ApiResponse.success("PIN updated."));
    }

    @PostMapping("/{childId}/pin/issue-temp")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Issue temporary PIN", description = "Issue one-time visible temporary PIN.")
    public ResponseEntity<ApiResponse<PinIssueResponseDTO>> issueTemporaryPin(
            Authentication authentication,
            @PathVariable UUID childId
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        String pin = childService.issueTemporaryPin(childId, userId);
        return ResponseEntity.ok(ApiResponse.success(
                "Temporary PIN issued.",
                PinIssueResponseDTO.builder().pin(pin).build()
        ));
    }

    @PostMapping("/{childId}/pin/verify")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "Verify PIN", description = "Verify PIN for child.")
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
    @Operation(summary = "Verify PIN and start game", description = "Verify PIN and issue game session token.")
    public ResponseEntity<ApiResponse<GameSessionDTO>> verifyPinAndStartGame(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody PinVerificationDTO verificationDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        GameSessionDTO session = childService.verifyPinAndCreateSession(childId, userId, verificationDTO);
        return ResponseEntity.ok(ApiResponse.success("Game session created.", session));
    }

    @DeleteMapping("/{childId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Remove PIN", description = "Remove current PIN after PIN verification.")
    public ResponseEntity<ApiResponse<Void>> removePin(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestParam String currentPin
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.removePin(childId, userId, currentPin);
        return ResponseEntity.ok(ApiResponse.success("PIN removed."));
    }

    @PostMapping("/{childId}/transfer-primary")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Transfer primary parent", description = "Transfer primary parent role to another parent.")
    public ResponseEntity<ApiResponse<Void>> transferPrimaryParent(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody TransferPrimaryParentDTO transferDTO
    ) {
        UUID userId = SecurityContextUtil.getCurrentUserId(authentication);
        childService.transferPrimaryParent(childId, userId, transferDTO);
        return ResponseEntity.ok(ApiResponse.success("Primary parent transferred."));
    }
}

