package myexpressionfriend_api.scenario.dto;

import java.util.List;

public record ScenarioSeedBatchGenerateResponseDTO(
        String character,
        int startIndex,
        int endIndex,
        int requestedCount,
        int generatedCount,
        int savedCount,
        int skippedCount,
        String backupPath,
        List<Item> items
) {
    public record Item(
            int rowIndex,
            String scenarioId,
            String status,
            String errorMessage
    ) {
    }
}
