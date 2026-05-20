package myexpressionfriend_api.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import myexpressionfriend_api.game.domain.ScenarioSource;
import myexpressionfriend_api.scenario.domain.Scenario;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScenarioStatusResponseDTO(
        @JsonProperty("scenario_id")
        String scenarioId,

        ScenarioSource source,

        @JsonProperty("approval_status")
        ScenarioApprovalStatus approvalStatus,

        @JsonProperty("reviewed_by_user_id")
        UUID reviewedByUserId,

        @JsonProperty("review_note")
        String reviewNote,

        @JsonProperty("published_at")
        LocalDateTime publishedAt,

        @JsonProperty("archived_at")
        LocalDateTime archivedAt
) {
    public static ScenarioStatusResponseDTO from(Scenario scenario) {
        return new ScenarioStatusResponseDTO(
                scenario.getScenarioId(),
                scenario.getSource(),
                scenario.getApprovalStatus(),
                scenario.getReviewedByUserId(),
                scenario.getReviewNote(),
                scenario.getPublishedAt(),
                scenario.getArchivedAt()
        );
    }
}
