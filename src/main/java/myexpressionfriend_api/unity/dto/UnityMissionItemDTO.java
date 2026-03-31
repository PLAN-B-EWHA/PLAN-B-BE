package myexpressionfriend_api.unity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
