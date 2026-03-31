package myexpressionfriend_api.unity.dto.prompt;

import lombok.*;

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
