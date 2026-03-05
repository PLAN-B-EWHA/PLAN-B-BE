package com.planB.myexpressionfriend.common.dto.child;

import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProfileUpdateDTO {

    @Size(max = 3000, message = "진단 정보는 3000자 이하여야 합니다.")
    private String diagnosisInfo;

    @Size(max = 3000, message = "특이사항은 3000자 이하여야 합니다.")
    private String specialNotes;

    private Set<ExpressionTag> preferredExpressions;
    private Set<ExpressionTag> difficultExpressions;
}
