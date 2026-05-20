package myexpressionfriend_api.player.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class GamePlayerSelectionRequestDTO {

    @NotNull(message = "childId is required")
    private UUID childId;
}