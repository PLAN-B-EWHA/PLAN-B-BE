package myexpressionfriend_api.game.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record ExpressionResultSaveRequestDTO(

        @NotBlank
        @JsonProperty("emotion_target")
        String emotionTarget,

        @NotNull
        @JsonProperty("started_at")
        Instant startedAt,

        @NotNull
        @JsonProperty("ended_at")
        Instant endedAt,

        @NotNull
        @JsonProperty("final_accuracy")
        Float finalAccuracy,

        @NotNull
        @JsonProperty("is_success")
        Boolean isSuccess,

        @NotEmpty
        @Valid
        @JsonProperty("tries")
        List<TryDTO> tries
) {
    public record TryDTO(

            @NotNull
            @JsonProperty("try_number")
            Integer tryNumber,

            @NotNull
            @JsonProperty("duration_ms")
            Integer durationMs,

            @NotNull
            @JsonProperty("accuracy_score")
            Float accuracyScore,

            @NotNull
            @JsonProperty("is_success")
            Boolean isSuccess
    ) {}
}
