package myexpressionfriend_api.unity.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionImportResultDTO {

    private int requestedCount;
    private int savedCount;
    private List<Long> savedIds;
}
