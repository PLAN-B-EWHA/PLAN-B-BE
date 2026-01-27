package com.planB.myexpressionfriend.common.dto;

import com.planB.myexpressionfriend.common.domain.Child;
import com.planB.myexpressionfriend.common.domain.ChildPermissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 아동 응답 DTO
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDTO {

    private UUID childId;
    private String name;
    private LocalDate birthDate;
    private String gender;
    private LocalDate diagnosisDate;
    private Boolean pinEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 현재 사용자의 권한 정보 (조회자 기준)
     */
    private Set<ChildPermissionType> myPermissions;
    private Boolean isPrimaryParent;
    private Boolean canPlay;    // PLAY_GAME 권한 여부
    private Boolean canManage;  // MANAGE 권한 여부

    /**
     * Entity → DTO 변환
     */
    public static ChildDTO from(Child child) {
        return ChildDTO.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .gender(child.getGender())
                .diagnosisDate(child.getDiagnosisDate())
                .pinEnabled(child.getPinEnabled())
                .createdAt(child.getCreatedAt())
                .updatedAt(child.getUpdatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (권한 정보 포함)
     */
    public static ChildDTO from(Child child, UUID currentUserId) {
        Set<ChildPermissionType> permissions = getPermissions(child, currentUserId);
        boolean isPrimary = child.isPrimaryParent(currentUserId);

        return ChildDTO.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .gender(child.getGender())
                .diagnosisDate(child.getDiagnosisDate())
                .pinEnabled(child.getPinEnabled())
                .createdAt(child.getCreatedAt())
                .updatedAt(child.getUpdatedAt())
                .myPermissions(permissions)
                .isPrimaryParent(isPrimary)
                .canPlay(permissions.contains(ChildPermissionType.PLAY_GAME) || isPrimary)
                .canManage(permissions.contains(ChildPermissionType.MANAGE) || isPrimary)
                .build();
    }


    /**
     * 사용자의 권한 목록 조회
     */
    private static Set<ChildPermissionType> getPermissions(Child child, UUID userId) {
        return child.getAuthorizedUsers().stream()
                .filter(au -> au.getUser().getUserId().equals(userId))
                .filter(au -> au.getIsActive())
                .findFirst()
                .map(au -> au.getPermissions())
                .orElse(Set.of());
    }
}
