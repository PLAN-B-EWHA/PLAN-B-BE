package myexpressionfriend_api.scenario.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import myexpressionfriend_api.scenario.domain.DialogueOption;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DialogueOptionDTO(
        Integer score,
        String text,
        @JsonProperty("peers_logic")  String peersLogic,
        String feedback,
        @JsonProperty("npc_reaction") List<String> npcReaction,
        @JsonProperty("reaction_animation") String reactionAnimation,
        @JsonProperty("reaction_expression") String reactionExpression
) {
    public static DialogueOptionDTO from(DialogueOption o) {
        return new DialogueOptionDTO(
                o.getScore(),
                o.getText(),
                o.getPeersLogic(),
                o.getFeedback(),
                o.getNpcReaction(),
                o.getReactionAnimation(),
                o.getReactionExpression()
        );
    }
}
