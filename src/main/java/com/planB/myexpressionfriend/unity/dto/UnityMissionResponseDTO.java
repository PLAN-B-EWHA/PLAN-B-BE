package com.planB.myexpressionfriend.unity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.planB.myexpressionfriend.unity.domain.UnityMission;
import com.planB.myexpressionfriend.unity.domain.UnityMissionApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionResponseDTO {

    private Long unityMissionId;
    private UUID childId;
    private Integer missionId;
    private String missionName;
    private String missionTypeString;
    private String targetKeyword;
    private String targetEmotionString;

    @JsonProperty("expression_data")
    private JsonNode expressionData;

    @JsonProperty("situation_data")
    private JsonNode situationData;

    private LocalDate missionDate;
    private UnityMissionApprovalStatus approvalStatus;
    private LocalDateTime createdAt;

    public static UnityMissionResponseDTO from(UnityMission mission) {
        return UnityMissionResponseDTO.builder()
                .unityMissionId(mission.getUnityMissionId())
                .childId(mission.getChild() != null ? mission.getChild().getChildId() : null)
                .missionId(mission.getMissionId())
                .missionName(mission.getMissionName())
                .missionTypeString(mission.getMissionTypeString())
                .targetKeyword(mission.getTargetKeyword())
                .targetEmotionString(mission.getTargetEmotionString())
                .expressionData(mission.getExpressionData())
                .situationData(mission.getSituationData())
                .missionDate(mission.getMissionDate())
                .approvalStatus(mission.getApprovalStatus())
                .createdAt(mission.getCreatedAt())
                .build();
    }
}
