package myexpressionfriend_api.realtime.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.realtime.config.OpenAiRealtimeProperties;
import myexpressionfriend_api.realtime.dto.RealtimeClientSecretResponse;
import myexpressionfriend_api.realtime.exception.RealtimeClientSecretException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiRealtimeService {

    private final OpenAiRealtimeProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public RealtimeClientSecretResponse createClientSecret() {
        validateConfiguration();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(), headers()),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String clientSecret = root.path("value").asText(null);
            Long expiresAt = root.path("expires_at").canConvertToLong()
                    ? root.path("expires_at").asLong()
                    : null;

            if (clientSecret == null || clientSecret.isBlank()) {
                throw new RealtimeClientSecretException(
                        HttpStatus.BAD_GATEWAY,
                        "OPENAI_REALTIME_INVALID_RESPONSE",
                        "OpenAI realtime client secret response did not contain a client secret."
                );
            }

            return new RealtimeClientSecretResponse(
                    clientSecret,
                    expiresAt,
                    properties.getModel(),
                    root.path("session")
            );
        } catch (HttpStatusCodeException ex) {
            log.warn("OpenAI realtime client secret request failed. status={}, body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new RealtimeClientSecretException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_REALTIME_UPSTREAM_ERROR",
                    "Failed to create OpenAI realtime client secret."
            );
        } catch (RealtimeClientSecretException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("OpenAI realtime client secret request failed.", ex);
            throw new RealtimeClientSecretException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_REALTIME_ERROR",
                    "Failed to create OpenAI realtime client secret."
            );
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new RealtimeClientSecretException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_REALTIME_DISABLED",
                    "OpenAI realtime is disabled."
            );
        }

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new RealtimeClientSecretException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_REALTIME_API_KEY_MISSING",
                    "OpenAI API key is not configured."
            );
        }
    }

    private String endpoint() {
        return properties.getBaseUrl().replaceAll("/+$", "") + "/v1/realtime/client_secrets";
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        return headers;
    }

    private Map<String, Object> requestBody() {
        Map<String, Object> audioOutput = new LinkedHashMap<>();
        audioOutput.put("voice", properties.getVoice());

        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("output", audioOutput);

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("type", "realtime");
        session.put("model", properties.getModel());
        session.put("output_modalities", outputModalities());
        session.put("instructions", properties.getInstructions());
        session.put("audio", audio);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session", session);
        return body;
    }

    private List<String> outputModalities() {
        String configuredModalities = properties.getModalities();
        if (configuredModalities == null || configuredModalities.isBlank()) {
            return List.of("text");
        }

        List<String> modalities = Arrays.stream(configuredModalities.split(","))
                .map(String::trim)
                .filter(modality -> !modality.isBlank())
                .toList();
        return modalities.isEmpty() ? List.of("text") : modalities;
    }
}
