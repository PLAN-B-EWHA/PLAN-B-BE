package myexpressionfriend_api.unity.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityGameResultSaveRequestDTO {

    @NotBlank
    private String sessionToken;

    @NotNull
    private Integer missionId;

    @NotNull
    private Boolean success;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    @NotNull
    @Min(0)
    private Float durationSeconds;

    @NotNull
    @Min(0)
    private Integer retryCount;
}
