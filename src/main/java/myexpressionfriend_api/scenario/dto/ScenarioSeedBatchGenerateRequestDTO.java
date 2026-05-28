package myexpressionfriend_api.scenario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ScenarioSeedBatchGenerateRequestDTO(
        @NotBlank
        String character,

        @Min(0)
        Integer startIndex,

        @Min(1)
        Integer endIndex,

        Integer topK,

        Double similarityThreshold,

        Boolean useProModel,

        String think,

        Boolean persistToDb,

        Boolean writeBackupJson
) {
}
