package myexpressionfriend_api.unity.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityGameResultSaveResponseDTO {

    private Long savedId;
    private Integer missionId;
    private UUID childId;
    private LocalDateTime createdAt;
}
