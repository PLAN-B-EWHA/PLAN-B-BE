package com.planB.myexpressionfriend.unity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Unity 미션 생성 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionGenerateRequestDTO {

    @NotNull
    private UUID childId;

    @NotNull
    @Builder.Default
    private UnityMissionGenerationType generationType = UnityMissionGenerationType.EXPRESSION;

    @Min(1)
    @Builder.Default
    private Integer missionIdStart = 1;

    @Min(1)
    @Builder.Default
    private Integer maxTokens = 4000;

    @Builder.Default
    private String modelName = "default";
}
