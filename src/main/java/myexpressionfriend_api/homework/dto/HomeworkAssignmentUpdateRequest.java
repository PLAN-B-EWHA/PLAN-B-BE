package myexpressionfriend_api.homework.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import myexpressionfriend_api.homework.domain.StrategyFocus;
import myexpressionfriend_api.homework.domain.StrategyTipSource;

import java.time.LocalDate;

public record HomeworkAssignmentUpdateRequest(
        @Min(1)
        @Max(16)
        Integer week,

        StrategyFocus strategyFocus,

        String instruction,

        String strategyTip,

        StrategyTipSource strategyTipSource,

        LocalDate dueDate
) {
}
