package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.MissionReviewDecision;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class MissionBatchReviewRequestDTO {

    @NotEmpty(message = "미션 ID 목록은 비어 있을 수 없습니다.")
    private List<UUID> missionIds;

    @NotNull(message = "검토 결정값은 필수입니다.")
    private MissionReviewDecision reviewDecision;

    @Size(max = 5000, message = "치료사 피드백은 5,000자를 초과할 수 없습니다.")
    private String therapistFeedback;
}

