package myexpressionfriend_api.player.dto;

import lombok.Builder;
import lombok.Getter;
import myexpressionfriend_api.player.domain.GamePlayerSelection;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GamePlayerSelectionResponseDTO {

    private UUID userId;
    private UUID childId;
    private String childName;
    private LocalDateTime selectedAt;
    private LocalDateTime updatedAt;

    public static GamePlayerSelectionResponseDTO from(GamePlayerSelection selection) {
        return GamePlayerSelectionResponseDTO.builder()
                .userId(selection.getUser().getUserId())
                .childId(selection.getChild().getChildId())
                .childName(selection.getChild().getName())
                .selectedAt(selection.getSelectedAt())
                .updatedAt(selection.getUpdatedAt())
                .build();
    }
}