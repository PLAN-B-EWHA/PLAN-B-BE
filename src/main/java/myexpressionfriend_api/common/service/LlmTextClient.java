package myexpressionfriend_api.common.service;

import java.util.Optional;

public interface LlmTextClient {
    Optional<String> generateText(String model, String prompt);
}
