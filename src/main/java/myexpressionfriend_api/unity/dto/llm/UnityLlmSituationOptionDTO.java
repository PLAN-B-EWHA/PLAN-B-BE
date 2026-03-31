package myexpressionfriend_api.unity.dto.llm;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmSituationOptionDTO {

    @NotNull
    @Positive
    private Integer id;

    @NotNull
    @Size(min = 1, max = 30)
    private String text;

    @NotNull
    private Boolean isCorrect;

    @NotEmpty
    private List<@Size(min = 1, max = 30) String> feedback;
}
