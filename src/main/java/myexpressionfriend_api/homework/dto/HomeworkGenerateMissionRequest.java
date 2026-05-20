package myexpressionfriend_api.homework.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import myexpressionfriend_api.homework.domain.StrategyFocus;

import java.time.LocalDate;

public record HomeworkGenerateMissionRequest(
        @Min(1)
        @Max(16)
        Integer week,

        StrategyFocus strategyFocus,

        @NotBlank
        String request,

        String retrievalQuery,

        String childSummary,

        String additionalContext,

        String templateKey,

        Integer topK,

        Double similarityThreshold,

        Boolean useProModel,

        LocalDate dueDate
) {
}
