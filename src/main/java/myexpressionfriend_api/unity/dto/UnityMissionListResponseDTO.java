package myexpressionfriend_api.unity.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionListResponseDTO {

    private List<UnityMissionResponseDTO> missions;
}
