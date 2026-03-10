package com.planB.myexpressionfriend.unity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unity 게임 결과 저장 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityGameResultSaveRequestDTO {

    @NotNull
    private Integer missionId;

    @NotNull
    private Boolean success;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    @NotNull
    @Min(0)
    private Float durationSeconds;

    @NotNull
    @Min(0)
    private Integer retryCount;
}