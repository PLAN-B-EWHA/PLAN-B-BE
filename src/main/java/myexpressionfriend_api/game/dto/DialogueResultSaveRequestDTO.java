package myexpressionfriend_api.game.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import myexpressionfriend_api.common.domain.PeersTheme;

import java.time.Instant;
import java.util.List;

public record DialogueResultSaveRequestDTO(

        @NotNull
        @JsonProperty("scenario_id")
        String scenarioId,

        @NotNull
        @JsonProperty("theme")
        PeersTheme theme,

        @NotNull
        @JsonProperty("started_at")
        Instant startedAt,

        @NotNull
        @JsonProperty("ended_at")
        Instant endedAt,

        @NotNull
        @PositiveOrZero
        @JsonProperty("total_score")
        Integer totalScore,

        @NotNull
        @JsonProperty("max_score")
        Integer maxScore,

        @NotEmpty
        @Valid
        @JsonProperty("turns")
        List<TurnDTO> turns
) {
    public record TurnDTO(

            @NotNull
            @JsonProperty("turn_id")
            Integer turnNumber,

            @NotNull
            @JsonProperty("selected_option_order")
            Integer selectedOptionOrder,

            @NotNull
            @JsonProperty("selected_score")
            Integer selectedScore
    ) {}
}
