package myexpressionfriend_api.statistics.dialogue.errorpattern.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.config.LlmProperties;
import myexpressionfriend_api.common.service.LlmTextClient;
import myexpressionfriend_api.statistics.dialogue.errorpattern.domain.ErrorPatternType;
import myexpressionfriend_api.statistics.dialogue.errorpattern.repository.ZeroScoreTurnProjection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmErrorPatternClassifier {

    private final LlmProperties llmProperties;
    private final LlmTextClient llmTextClient;

    public List<TurnClassification> classifyInTriplicate(List<ZeroScoreTurnProjection> turns, boolean useProModel) {
        List<TurnClassification> results = new ArrayList<>();
        String model = useProModel ? llmProperties.getModelPro() : llmProperties.getModelFlash();

        for (ZeroScoreTurnProjection turn : turns) {
            ErrorPatternType p1 = classifySingle(turn, model);
            ErrorPatternType p2 = classifySingle(turn, model);
            ErrorPatternType p3 = classifySingle(turn, model);

            ErrorPatternType finalType = majorityVote(p1, p2, p3);
            boolean consistent = isConsistent(p1, p2, p3);
            results.add(new TurnClassification(turn, finalType, consistent));
        }
        return results;
    }

    private ErrorPatternType classifySingle(ZeroScoreTurnProjection turn, String model) {
        try {
            return callLlmApi(turn, model);
        } catch (Exception ex) {
            log.warn("LLM classify failed. turnId={}, message={}", turn.getTurnId(), ex.getMessage());
            return ErrorPatternType.UNCLASSIFIED;
        }
    }

    private ErrorPatternType callLlmApi(ZeroScoreTurnProjection turn, String model) {
        String prompt = """
                You are a classifier for social dialogue errors.
                Return exactly one label from:
                LECTURING, CRITICISM, TOPIC_IGNORE, REJECTION, UNCLASSIFIED.

                Input:
                scenario_id=%s
                theme=%s
                turn_number=%s
                selected_option_order=%s

                Output format:
                LABEL_ONLY
                """.formatted(
                safe(turn.getScenarioId()),
                safe(turn.getTheme() != null ? turn.getTheme().name() : null),
                safe(turn.getTurnNumber()),
                safe(turn.getSelectedOptionOrder())
        );

        Optional<String> response = llmTextClient.generateText(model, prompt);
        if (response.isEmpty()) {
            return ErrorPatternType.UNCLASSIFIED;
        }
        return parseLabel(response.get());
    }

    private ErrorPatternType parseLabel(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("LECTURING")) return ErrorPatternType.LECTURING;
        if (normalized.contains("CRITICISM")) return ErrorPatternType.CRITICISM;
        if (normalized.contains("TOPIC_IGNORE")) return ErrorPatternType.TOPIC_IGNORE;
        if (normalized.contains("REJECTION")) return ErrorPatternType.REJECTION;
        return ErrorPatternType.UNCLASSIFIED;
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private ErrorPatternType majorityVote(ErrorPatternType p1, ErrorPatternType p2, ErrorPatternType p3) {
        if (p1 == p2 || p1 == p3) return p1;
        if (p2 == p3) return p2;
        return ErrorPatternType.UNCLASSIFIED;
    }

    private boolean isConsistent(ErrorPatternType p1, ErrorPatternType p2, ErrorPatternType p3) {
        return p1 == p2 || p1 == p3 || p2 == p3;
    }

    public record TurnClassification(
            ZeroScoreTurnProjection turn,
            ErrorPatternType finalType,
            boolean consistent
    ) {}
}
