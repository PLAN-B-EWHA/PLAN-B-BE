package com.planB.myexpressionfriend.common.dto.child;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
import com.planB.myexpressionfriend.common.domain.child.LanguageSkill;
import com.planB.myexpressionfriend.common.domain.child.SensoryProcessing;
import com.planB.myexpressionfriend.common.dto.user.UserBasicDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 아동 상세 정보 DTO (권한 사용자 목록 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDetailDTO {

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
     * 주보호자 정보
     */
    private UserBasicDTO primaryParent;

    /**
     * 권한 부여된 사용자 목록
     */
    private List<AuthorizedUserDTO> authorizedUsers;

    /**
     * Entity -> DTO 변환
     */
    public static ChildDetailDTO from(Child child) {
        UserBasicDTO primaryParent = child.getPrimaryParent()
                .map(UserBasicDTO::from)
                .orElse(null);

        List<AuthorizedUserDTO> authorizedUsers = child.getAuthorizedUsers().stream()
                .filter(au -> au.getIsActive())
                .map(AuthorizedUserDTO::from)
                .collect(Collectors.toList());

        return ChildDetailDTO.builder()
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
}
