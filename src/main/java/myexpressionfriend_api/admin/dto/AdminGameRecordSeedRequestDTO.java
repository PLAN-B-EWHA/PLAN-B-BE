package myexpressionfriend_api.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import myexpressionfriend_api.common.domain.PeersTheme;

import java.time.LocalDate;
import java.util.UUID;

public record AdminGameRecordSeedRequestDTO(
        @NotNull
        UUID childId,

        PeersTheme theme,

        String emotionTarget,

        @Positive
        Integer dialogueSessionCount,

        @Positive
        Integer expressionSessionCount,

        LocalDate startDate
) {
    public PeersTheme themeOrDefault() {
        return theme == null ? PeersTheme.INFORMATION_EXCHANGE : theme;
    }

    public String emotionTargetOrDefault() {
        return emotionTarget == null || emotionTarget.isBlank() ? "happy" : emotionTarget.trim();
    }

    public int dialogueSessionCountOrDefault() {
        return dialogueSessionCount == null ? 5 : dialogueSessionCount;
    }

    public int expressionSessionCountOrDefault() {
        return expressionSessionCount == null ? 5 : expressionSessionCount;
    }

    public LocalDate startDateOrDefault() {
        return startDate == null ? LocalDate.now().minusDays(6) : startDate;
    }
}
