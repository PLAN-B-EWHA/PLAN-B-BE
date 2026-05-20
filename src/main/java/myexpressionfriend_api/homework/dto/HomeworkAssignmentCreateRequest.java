package myexpressionfriend_api.homework.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import myexpressionfriend_api.homework.domain.StrategyFocus;
import myexpressionfriend_api.homework.domain.StrategyTipSource;

import java.time.LocalDate;

public record HomeworkAssignmentCreateRequest(
        @Min(1)
        @Max(16)
        Integer week,

        StrategyFocus strategyFocus,

        @NotBlank
        String instruction,

        String strategyTip,

        StrategyTipSource strategyTipSource,

        LocalDate dueDate
) {
}
