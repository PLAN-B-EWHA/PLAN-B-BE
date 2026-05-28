package myexpressionfriend_api.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;

public record ScenarioStatusUpdateRequestDTO(
        @NotNull
        @JsonProperty("approval_status")
        ScenarioApprovalStatus approvalStatus,

        @JsonProperty("review_note")
        String reviewNote
) {
}
