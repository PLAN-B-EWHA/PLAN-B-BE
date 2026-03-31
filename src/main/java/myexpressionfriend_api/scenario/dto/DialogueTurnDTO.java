package myexpressionfriend_api.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import myexpressionfriend_api.scenario.domain.ScenarioDialogueTurn;

import java.util.List;

public record DialogueTurnDTO(
        @JsonProperty("turn_id")           Integer turnId,
        @JsonProperty("internal_monologue") String internalMonologue,
        @JsonProperty("npc_utterance")     List<String> npcUtterance,
        @JsonProperty("npc_animation")     String npcAnimation,
        @JsonProperty("npc_expression")    String npcExpression,
        List<DialogueOptionDTO> options
) {
    public static DialogueTurnDTO from(ScenarioDialogueTurn t) {
        return new DialogueTurnDTO(
                t.getTurnOrder(),
                t.getInternalMonologue(),
                t.getNpcUtterance(),
                t.getNpcAnimation(),
                t.getNpcExpression(),
                t.getOptions().stream().map(DialogueOptionDTO::from).toList()
        );
    }
}
