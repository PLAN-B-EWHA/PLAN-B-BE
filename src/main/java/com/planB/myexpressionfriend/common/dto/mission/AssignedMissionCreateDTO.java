package com.planB.myexpressionfriend.common.dto.mission;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AssignedMissionCreateDTO {

    @NotNull(message = "아동 ID는 필수입니다.")
    private UUID childId;

    @NotNull(message = "템플릿 ID는 필수입니다.")
    private UUID templateId;

    @Future(message = "목표 완료일은 미래 시각이어야 합니다.")
    private LocalDateTime dueDate;
}
