package myexpressionfriend_api.homework.dto;

import jakarta.validation.constraints.NotNull;
import myexpressionfriend_api.homework.domain.CompletionStatus;
import myexpressionfriend_api.homework.domain.InitiationType;
import myexpressionfriend_api.homework.domain.StrategyFocus;

public record HomeworkReportSubmitRequest(
        @NotNull
        CompletionStatus completed,

        InitiationType initiatedBy,

        StrategyFocus strategyApplied,

        String parentObservation,

        String peerResponseObserved,

        Boolean spontaneousFlag
) {
}
