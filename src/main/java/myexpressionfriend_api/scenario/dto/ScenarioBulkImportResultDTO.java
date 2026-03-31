package myexpressionfriend_api.scenario.dto;

public record ScenarioBulkImportResultDTO(
        int requestedCount,
        int savedCount,
        int skippedCount         // 이미 존재하는 scenario_id는 건너뜀 (idempotent)
) {}
