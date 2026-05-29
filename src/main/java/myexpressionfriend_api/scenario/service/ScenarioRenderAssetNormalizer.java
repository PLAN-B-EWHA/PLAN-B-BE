package myexpressionfriend_api.scenario.service;

import myexpressionfriend_api.scenario.dto.DialogueOptionDTO;
import myexpressionfriend_api.scenario.dto.DialogueTurnDTO;
import myexpressionfriend_api.scenario.dto.ScenarioDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ScenarioRenderAssetNormalizer {

    private static final String DEFAULT_ANIMATION = "Idle";
    private static final String DEFAULT_EXPRESSION = "Neutral";

    private static final Set<String> ALLOWED_ANIMATIONS = Set.of(
            "Acknowledging",
            "Angry_Gesture",
            "Dismissing_Gesture",
            "Happy_Hand_Gesture",
            "Thoughtful_Head_Shake",
            "Idle",
            "Head_Nod_Yes"
    );

    private static final Set<String> ALLOWED_EXPRESSIONS = Set.of(
            "Joy",
            "Sadness",
            "Surprise",
            "Anger",
            "Neutral"
    );

    public ScenarioDTO normalize(ScenarioDTO scenario) {
        if (scenario == null || scenario.dialogueFlow() == null) {
            return scenario;
        }

        List<DialogueTurnDTO> turns = scenario.dialogueFlow().stream()
                .map(this::normalizeTurn)
                .toList();

        return new ScenarioDTO(
                scenario.scenarioId(),
                scenario.isCompleted(),
                scenario.source(),
                scenario.approvalStatus(),
                scenario.metadata(),
                scenario.cast(),
                turns,
                scenario.finalSummary()
        );
    }

    private DialogueTurnDTO normalizeTurn(DialogueTurnDTO turn) {
        List<DialogueOptionDTO> options = turn.options() == null ? null
                : turn.options().stream()
                .map(this::normalizeOption)
                .toList();

        return new DialogueTurnDTO(
                turn.turnId(),
                turn.internalMonologue(),
                turn.npcUtterance(),
                normalizeAnimation(turn.npcAnimation()),
                normalizeExpression(turn.npcExpression()),
                options
        );
    }

    private DialogueOptionDTO normalizeOption(DialogueOptionDTO option) {
        return new DialogueOptionDTO(
                option.score(),
                option.text(),
                option.peersLogic(),
                option.feedback(),
                option.npcReaction(),
                normalizeAnimation(option.reactionAnimation()),
                normalizeExpression(option.reactionExpression())
        );
    }

    private String normalizeAnimation(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ANIMATION;
        }
        String trimmed = value.trim();
        return ALLOWED_ANIMATIONS.contains(trimmed) ? trimmed : DEFAULT_ANIMATION;
    }

    private String normalizeExpression(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_EXPRESSION;
        }
        String trimmed = value.trim();
        return ALLOWED_EXPRESSIONS.contains(trimmed) ? trimmed : DEFAULT_EXPRESSION;
    }
}
