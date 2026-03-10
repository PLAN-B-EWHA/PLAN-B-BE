package com.planB.myexpressionfriend.unity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.planB.myexpressionfriend.unity.domain.UnityMission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unity 미션 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionResponseDTO {

    private Long unityMissionId;
    private Integer missionId;
    private String missionName;
    private String missionTypeString;
    private String targetKeyword;
    private String targetEmotionString;

    @JsonProperty("expression_data")
    private JsonNode expressionData;

    @JsonProperty("situation_data")
    private JsonNode situationData;

    private LocalDateTime createdAt;

    /**
     * 엔티티를 응답 DTO로 변환합니다.
     *
     * @param mission Unity 미션 엔티티
     * @return UnityMissionResponseDTO
     */
    public static UnityMissionResponseDTO from(UnityMission mission) {
        return UnityMissionResponseDTO.builder()
                .unityMissionId(mission.getUnityMissionId())
                .missionId(mission.getMissionId())
                .missionName(mission.getMissionName())
                .missionTypeString(mission.getMissionTypeString())
                .targetKeyword(mission.getTargetKeyword())
                .targetEmotionString(mission.getTargetEmotionString())
                .expressionData(mission.getExpressionData())
                .situationData(mission.getSituationData())
                .createdAt(mission.getCreatedAt())
                .build();
    }
}