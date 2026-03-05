package com.planB.myexpressionfriend.common.dto.child;

import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

/**
 * 아동 정보 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildUpdateDTO {

    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "성별은 MALE, FEMALE, OTHER 중 하나여야 합니다.")
    private String gender;

    @Past(message = "진단일은 과거 날짜여야 합니다.")
    private LocalDate diagnosisDate;

    @Size(max = 3000, message = "진단 정보는 3000자 이하여야 합니다.")
    private String diagnosisInfo;

    @Size(max = 3000, message = "특이사항은 3000자 이하여야 합니다.")
    private String specialNotes;

    private Set<ExpressionTag> preferredExpressions;
    private Set<ExpressionTag> difficultExpressions;

    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하이어야 합니다.")
    private String profileImageUrl;
}
