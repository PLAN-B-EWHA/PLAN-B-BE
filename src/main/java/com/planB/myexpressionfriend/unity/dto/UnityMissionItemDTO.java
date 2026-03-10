package com.planB.myexpressionfriend.unity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unity 미션 항목 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionItemDTO {

    @NotNull
    private Integer missionId;

    @NotBlank
    private String missionName;

    @NotBlank
    private String missionTypeString;

    @NotBlank
    private String targetKeyword;

    @NotBlank
    private String targetEmotionString;

    @JsonProperty("expression_data")
    private JsonNode expressionData;

    @JsonProperty("situation_data")
    private JsonNode situationData;
}