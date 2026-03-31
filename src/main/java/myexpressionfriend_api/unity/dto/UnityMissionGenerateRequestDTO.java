package myexpressionfriend_api.unity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

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
