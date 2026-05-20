package myexpressionfriend_api.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminGameRecordSeedResponseDTO(
        UUID childId,
        int dialogueInsertedCount,
        int expressionInsertedCount,
        List<UUID> dialogueSessionIds,
        List<UUID> expressionSessionIds
) {
}
