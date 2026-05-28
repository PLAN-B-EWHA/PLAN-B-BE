package myexpressionfriend_api.common.service;

import java.util.Optional;

public interface LlmTextClient {
    default Optional<String> generateText(String model, String prompt) {
        return generateText(model, prompt, LlmGenerateOptions.none());
    }

    Optional<String> generateText(String model, String prompt, LlmGenerateOptions options);
}
