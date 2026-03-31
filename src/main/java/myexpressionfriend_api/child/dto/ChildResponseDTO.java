package myexpressionfriend_api.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.child.domain.*;

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
public class ChildResponseDTO {

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
    private LanguageSkill languageSkill;
    private SensoryProcessing sensoryProcessing;
    private String interests;
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
    public static ChildResponseDTO from(Child child) {
        return ChildResponseDTO.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .age(child.calculateAge())
                .gender(child.getGender())
                .diagnosisDate(child.getDiagnosisDate())
                .diagnosisInfo(child.getDiagnosisInfo())
                .specialNotes(child.getSpecialNotes())
                .preferredExpressions(copySet(child.getPreferredExpressions()))
                .difficultExpressions(copySet(child.getDifficultExpressions()))
                .languageSkill(child.getLanguageSkill())
                .sensoryProcessing(child.getSensoryProcessing())
                .interests(child.getInterests())
                .profileImageUrl(child.getProfileImageUrl())
                .pinEnabled(child.getPinEnabled())
                .createdAt(child.getCreatedAt())
                .updatedAt(child.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (권한 정보 포함)
     */
    public static ChildResponseDTO from(Child child, UUID currentUserId) {
        Set<ChildPermissionType> permissions = getPermissions(child, currentUserId);
        boolean isPrimary = child.isPrimaryParent(currentUserId);

        return ChildResponseDTO.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .age(child.calculateAge())
                .gender(child.getGender())
                .diagnosisDate(child.getDiagnosisDate())
                .diagnosisInfo(child.getDiagnosisInfo())
                .specialNotes(child.getSpecialNotes())
                .preferredExpressions(copySet(child.getPreferredExpressions()))
                .difficultExpressions(copySet(child.getDifficultExpressions()))
                .languageSkill(child.getLanguageSkill())
                .sensoryProcessing(child.getSensoryProcessing())
                .interests(child.getInterests())
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
                .filter(ChildrenAuthorizedUser::getIsActive)
                .findFirst()
                .map(au -> Boolean.TRUE.equals(au.getIsPrimary())
                        ? Set.of(ChildPermissionType.values())
                        : copySet(au.getPermissions()))
                .orElse(Set.of());
    }

    private static <T> Set<T> copySet(Set<T> source) {
        return source == null ? Set.of() : Set.copyOf(source);
    }
}
