package myexpressionfriend_api.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScenarioReviewRequestDTO(
        @JsonProperty("review_note")
        String reviewNote
) {
}
