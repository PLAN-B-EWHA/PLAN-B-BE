package myexpressionfriend_api.unity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionImportRequestDTO {

    @Valid
    @NotEmpty
    private List<UnityMissionItemDTO> missions;
}
