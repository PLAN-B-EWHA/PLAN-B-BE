package myexpressionfriend_api.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import myexpressionfriend_api.game.domain.ScenarioSource;
import myexpressionfriend_api.scenario.domain.Scenario;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminScenarioResponseDTO(
        @JsonProperty("scenario_id")
        String scenarioId,

        ScenarioSource source,

        @JsonProperty("approval_status")
        ScenarioApprovalStatus approvalStatus,

        Integer week,

        String theme,

        @JsonProperty("lobby_title")
        String lobbyTitle,

        @JsonProperty("main_character")
        String mainCharacter,

        @JsonProperty("reviewed_by_user_id")
        UUID reviewedByUserId,

        @JsonProperty("review_note")
        String reviewNote,

        @JsonProperty("published_at")
        LocalDateTime publishedAt,

        @JsonProperty("archived_at")
        LocalDateTime archivedAt,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {
    public static AdminScenarioResponseDTO from(Scenario scenario) {
        return new AdminScenarioResponseDTO(
                scenario.getScenarioId(),
                scenario.getSource(),
                scenario.getApprovalStatus(),
                scenario.getWeek(),
                scenario.getTheme(),
                scenario.getLobbyTitle(),
                scenario.getMainCharacter(),
                scenario.getReviewedByUserId(),
                scenario.getReviewNote(),
                scenario.getPublishedAt(),
                scenario.getArchivedAt(),
                scenario.getCreatedAt(),
                scenario.getUpdatedAt()
        );
    }
}
