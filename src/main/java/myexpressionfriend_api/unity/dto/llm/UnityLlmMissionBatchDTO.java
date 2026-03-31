package myexpressionfriend_api.unity.dto.llm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmMissionBatchDTO {

    @Valid
    @NotEmpty
    private List<UnityLlmMissionDTO> missions;
}
