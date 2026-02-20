package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionStatusUpdateDTO {

    private MissionStatus status;

    @Size(max = 5000, message = "부모 코멘트는 5,000자를 초과할 수 없습니다.")
    private String parentNote;

    @Size(max = 5000, message = "치료사 피드백은 5,000자를 초과할 수 없습니다.")
    private String therapistFeedback;
}
