package myexpressionfriend_api.unity.dto.llm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmSituationDataDTO {

    @NotEmpty
    private List<@NotBlank @Size(max = 30) String> situationDescription;

    @NotBlank
    @Size(max = 30)
    private String question;

    @Valid
    @NotEmpty
    @Size(min = 4, max = 4)
    private List<UnityLlmSituationOptionDTO> options;

    @AssertTrue(message = "Exactly one option must be marked as correct.")
    public boolean isSingleCorrectOption() {
        if (options == null) {
            return false;
        }
        return options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .count() == 1;
    }
}
