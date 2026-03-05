package com.planB.myexpressionfriend.common.dto.child;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
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
    private Integer age;
    private String gender;
    private LocalDate diagnosisDate;
    private String diagnosisInfo;
    private String specialNotes;
    private Set<ExpressionTag> preferredExpressions;
    private Set<ExpressionTag> difficultExpressions;
    private String profileImageUrl;
    private Boolean pinEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 현재 사용자의 권한 정보 (조회 기준)
     */
    private Set<ChildPermissionType> myPermissions;
    private Boolean isPrimaryParent;
    private Boolean canPlay;
    private Boolean canManage;

    /**
     * Entity -> DTO 변환
     */
    public static ChildDTO from(Child child) {
        return ChildDTO.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .age(child.calculateAge())
                .gender(child.getGender())
                .diagnosisDate(child.getDiagnosisDate())
                .diagnosisInfo(child.getDiagnosisInfo())
                .specialNotes(child.getSpecialNotes())
                .preferredExpressions(child.getPreferredExpressions())
                .difficultExpressions(child.getDifficultExpressions())
                .profileImageUrl(child.getProfileImageUrl())
                .pinEnabled(child.getPinEnabled())
                .createdAt(child.getCreatedAt())
                .updatedAt(child.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (권한 정보 포함)
     */
    public static ChildDTO from(Child child, UUID currentUserId) {
        Set<ChildPermissionType> permissions = getPermissions(child, currentUserId);
        boolean isPrimary = child.isPrimaryParent(currentUserId);

        return ChildDTO.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .age(child.calculateAge())
                .gender(child.getGender())
                .diagnosisDate(child.getDiagnosisDate())
                .diagnosisInfo(child.getDiagnosisInfo())
                .specialNotes(child.getSpecialNotes())
                .preferredExpressions(child.getPreferredExpressions())
                .difficultExpressions(child.getDifficultExpressions())
                .profileImageUrl(child.getProfileImageUrl())
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
