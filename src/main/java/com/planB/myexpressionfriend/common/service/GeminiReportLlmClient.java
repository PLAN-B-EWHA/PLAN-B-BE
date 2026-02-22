package com.planB.myexpressionfriend.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.config.GeminiProperties;
import com.planB.myexpressionfriend.common.exception.LlmQuotaExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiReportLlmClient implements ReportLlmClient {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @Override
    public String generateReport(String prompt, int maxTokens, String modelName) {
        if (!geminiProperties.isEnabled()) {
            throw new IllegalStateException("Gemini integration is disabled");
        }
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt is required");
        }

        String resolvedModel = resolveModel(modelName);

        String endpoint = String.format(
                "%s/v1beta/models/%s:generateContent?key=%s",
                geminiProperties.getBaseUrl(),
                resolvedModel,
                geminiProperties.getApiKey()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("temperature", 0.4);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));
        payload.put("generationConfig", generationConfig);

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(geminiProperties.getTimeoutMs());
            requestFactory.setReadTimeout(geminiProperties.getTimeoutMs());

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    URI.create(endpoint),
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Gemini API request failed");
            }

            JsonNode root = objectMapper.readTree(response.getBody().getBytes(StandardCharsets.UTF_8));
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException("Gemini API returned no candidates");
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new IllegalStateException("Gemini API returned empty content parts");
            }

            String text = parts.get(0).path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Gemini API returned empty text");
            }
            return text.trim();
        } catch (HttpClientErrorException.TooManyRequests e) {
            Integer retryAfterSeconds = extractRetryAfterSeconds(e.getResponseBodyAsString());
            log.warn("Gemini quota exceeded. model={}, retryAfterSeconds={}",
                    resolvedModel, retryAfterSeconds);
            throw new LlmQuotaExceededException(
                    "AI 리포트 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
                    retryAfterSeconds
            );
        } catch (Exception e) {
            log.error("Gemini report generation failed. model={}", resolvedModel, e);
            throw new IllegalStateException("Failed to generate report with Gemini: " + e.getMessage());
        }
    }

    private String resolveModel(String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank() || "default".equalsIgnoreCase(requestedModel)) {
            return geminiProperties.getModel();
        }
        return requestedModel.trim();
    }

    private Integer extractRetryAfterSeconds(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        Pattern[] patterns = new Pattern[]{
                Pattern.compile("\"retryDelay\"\\s*:\\s*\"(\\d+)s\""),
                Pattern.compile("Please retry in\\s+(\\d+)"),
                Pattern.compile("retry in\\s+(\\d+)s", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
