package myexpressionfriend_api.unity.dto.llm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmExpressionDataDTO {

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> characterDialogue;

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> successFeedback;

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> retryFeedback;

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> failFeedback;
}
