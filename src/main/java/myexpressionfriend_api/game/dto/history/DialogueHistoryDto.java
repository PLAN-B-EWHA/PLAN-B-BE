package myexpressionfriend_api.game.dto.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import myexpressionfriend_api.game.domain.ScenarioSource;

public record DialogueHistoryDto(
        UUID sessionId,
        String theme,
        String scenarioId,
        ScenarioSource scenarioSource,
        Instant playedAt,
        long durationSeconds,
        int totalScore,
        int maxScore,
        double scoreRate,
        List<TurnDto> turns
) {
    public record TurnDto(
            int turnNumber,
            int selectedOptionOrder,
            int selectedScore,
            String npcReactionExpression
    ) {}
}
