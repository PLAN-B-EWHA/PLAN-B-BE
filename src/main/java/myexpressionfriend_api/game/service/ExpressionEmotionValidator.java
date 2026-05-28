package myexpressionfriend_api.game.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.config.ExpressionBaselineProperties;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
public class ExpressionEmotionValidator {

    private static final Set<String> DEFAULT_ALLOWED = Set.of(
            "happy", "sad", "angry", "disgust", "surprise", "fear");

    private final ExpressionBaselineProperties baselineProperties;

    public String normalizeAndValidate(String emotionTarget) {
        if (emotionTarget == null || emotionTarget.isBlank()) {
            throw new InvalidRequestException("emotionTarget is required.");
        }

        String normalized = emotionTarget.trim().toLowerCase();
        Set<String> allowed = allowedEmotions();
        if (!allowed.contains(normalized)) {
            throw new InvalidRequestException(
                    "Unsupported emotionTarget: " + emotionTarget + ". allowed=" + allowed);
        }
        return normalized;
    }

    public Set<String> allowedEmotions() {
        Set<String> configured = new TreeSet<>();
        baselineProperties.getBaselineDurationMs().keySet().stream()
                .filter(key -> key != null && !key.isBlank())
                .map(key -> key.trim().toLowerCase())
                .forEach(configured::add);
        return configured.isEmpty() ? new TreeSet<>(DEFAULT_ALLOWED) : configured;
    }
}
