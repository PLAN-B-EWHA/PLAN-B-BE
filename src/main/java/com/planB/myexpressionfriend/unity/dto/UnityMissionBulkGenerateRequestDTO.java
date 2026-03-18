package com.planB.myexpressionfriend.unity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionBulkGenerateRequestDTO {

    @NotNull
    private UUID childId;

    @Min(0) @Max(5)
    @Builder.Default
    private int expressionCount = 2;

    @Min(0) @Max(5)
    @Builder.Default
    private int situationCount = 3;

    @Min(1)
    @Builder.Default
    private int maxTokens = 4000;

    @Builder.Default
    private String modelName = "default";
}
