package com.planB.myexpressionfriend.unity.dto.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Unity 미션 프롬프트 컨텍스트 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionPromptContextDTO {

    private String childName;
    private String childInterests;
    private String childProfileSummary;
    private String abcObservationSummary;
    private String characterAffinityLevel;
    private String defaultAffinityLevel;
    private String recentUnitySummary;
    private String allowedTargetEmotions;
    private String missionIdStart;
    private String missionIdNext;
}
