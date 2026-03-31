package myexpressionfriend_api.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.auth.dto.UserBasicDTO;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ExpressionTag;
import myexpressionfriend_api.child.domain.LanguageSkill;
import myexpressionfriend_api.child.domain.SensoryProcessing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 아동 상세 정보 DTO (권한 사용자 목록 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDetailResponseDTO {

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

    /** 주보호자 정보 */
    private UserBasicDTO primaryParent;

    /** 권한 부여된 사용자 목록 (활성화된 것만) */
    private List<AuthorizedUserResponseDTO> authorizedUsers;

    /**
     * Entity -> DTO 변환
     */
    public static ChildDetailResponseDTO from(Child child) {
        UserBasicDTO primaryParent = child.getPrimaryParent()
                .map(UserBasicDTO::from)
                .orElse(null);

        List<AuthorizedUserResponseDTO> authorizedUsers = child.getAuthorizedUsers().stream()
                .filter(au -> Boolean.TRUE.equals(au.getIsActive()))
                .map(AuthorizedUserResponseDTO::from)
                .toList();

        return ChildDetailResponseDTO.builder()
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
                .primaryParent(primaryParent)
                .authorizedUsers(authorizedUsers)
                .build();
    }

    private static <T> Set<T> copySet(Set<T> source) {
        return source == null ? Set.of() : Set.copyOf(source);
    }
}
