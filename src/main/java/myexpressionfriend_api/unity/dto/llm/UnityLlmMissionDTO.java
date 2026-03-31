package myexpressionfriend_api.unity.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmMissionDTO {

    @NotNull
    @Positive
    private Integer missionId;

    @NotBlank
    @Size(max = 200)
    private String missionName;

    @NotBlank
    @Pattern(regexp = "Expression|Situation")
    private String missionTypeString;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]*$", message = "targetKeyword must be English letters or digits only.")
    private String targetKeyword;

    @NotBlank
    @Size(max = 100)
    private String targetEmotionString;

    @Valid
    @JsonProperty("expression_data")
    private UnityLlmExpressionDataDTO expressionData;

    @Valid
    @JsonProperty("situation_data")
    private UnityLlmSituationDataDTO situationData;

    @AssertTrue(message = "Expression missions require expression_data only, and Situation missions require situation_data only.")
    public boolean isDetailPayloadValid() {
        if ("Expression".equals(missionTypeString)) {
            return expressionData != null && situationData == null;
        }
        if ("Situation".equals(missionTypeString)) {
            return situationData != null && expressionData == null;
        }
        return false;
    }
}
