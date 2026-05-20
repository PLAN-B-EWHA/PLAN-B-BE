package myexpressionfriend_api.game.dto.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExpressionHistoryDto(
        UUID sessionId,
        String emotion,
        Instant playedAt,
        double finalAccuracy,
        boolean isSuccess,
        int totalTries,
        List<TryDto> tries
) {
    public record TryDto(
            int tryNumber,
            double accuracyScore,
            int durationMs,
            boolean isSuccess
    ) {}
}
