package myexpressionfriend_api.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.config.LlmProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmApiClient implements LlmTextClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Optional<String> generateText(String model, String prompt, LlmGenerateOptions options) {
        if (!llmProperties.isEnabled()) {
            return Optional.empty();
        }

        for (int attempt = 0; attempt <= llmProperties.getMaxRetries(); attempt++) {
            try {
                return callGenerate(model, prompt, options);
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() == 429 && attempt < llmProperties.getMaxRetries()) {
                    sleepBackoff(attempt);
                    continue;
                }
                log.warn("LLM call failed. status={}, message={}", ex.getStatusCode().value(), ex.getMessage());
                return Optional.empty();
            } catch (Exception ex) {
                if (attempt < llmProperties.getMaxRetries()) {
                    sleepBackoff(attempt);
                    continue;
                }
                log.warn("LLM call failed. message={}", ex.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<String> callGenerate(String model, String prompt, LlmGenerateOptions options) throws Exception {
        String endpoint = llmProperties.getBaseUrl().replaceAll("/+$", "") + "/api/generate";
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("stream", false);
        Object think = resolveThink(options);
        if (think != null) {
            body.put("think", think);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (llmProperties.getApiKey() != null && !llmProperties.getApiKey().isBlank()) {
            headers.setBearerAuth(llmProperties.getApiKey());
        }

        ResponseEntity<String> response = restTemplate.exchange(
                endpoint, HttpMethod.POST, new HttpEntity<>(body, headers), String.class
        );
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode textNode = root.path("response");
        if (textNode.isMissingNode() || textNode.isNull()) return Optional.empty();
        return Optional.ofNullable(textNode.asText());
    }

    private Object resolveThink(LlmGenerateOptions options) {
        String value = options == null ? null : options.think();
        if (value == null || value.isBlank()) {
            value = llmProperties.getThink();
        }
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        return normalized;
    }

    private void sleepBackoff(int attempt) {
        int seconds = Math.min(1 << attempt, llmProperties.getMaxBackoffSeconds());
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
