package myexpressionfriend_api.realtime.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.realtime.config.OpenAiRealtimeProperties;
import myexpressionfriend_api.realtime.dto.RealtimeAdminConfigResponse;
import myexpressionfriend_api.realtime.dto.RealtimeAdminConfigUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OpenAiRealtimeAdminConfigService {

    private static final Set<String> ALLOWED_MODALITIES = Set.of("text", "audio");

    private final OpenAiRealtimeProperties properties;

    public synchronized RealtimeAdminConfigResponse getConfig() {
        return response();
    }

    public synchronized RealtimeAdminConfigResponse updateConfig(RealtimeAdminConfigUpdateRequest request) {
        if (request.enabled() != null) {
            properties.setEnabled(request.enabled());
        }
        if (request.baseUrl() != null) {
            properties.setBaseUrl(requiredTrimmed(request.baseUrl(), "baseUrl"));
        }
        if (request.model() != null) {
            properties.setModel(requiredTrimmed(request.model(), "model"));
        }
        if (request.voice() != null) {
            properties.setVoice(requiredTrimmed(request.voice(), "voice"));
        }
        if (request.modalities() != null) {
            properties.setModalities(String.join(",", validateModalities(request.modalities())));
        }
        if (request.instructions() != null) {
            properties.setInstructions(requiredTrimmed(request.instructions(), "instructions"));
        }
        return response();
    }

    private RealtimeAdminConfigResponse response() {
        return new RealtimeAdminConfigResponse(
                properties.isEnabled(),
                properties.getBaseUrl(),
                properties.getModel(),
                properties.getVoice(),
                parseModalities(properties.getModalities()),
                properties.getInstructions()
        );
    }

    private String requiredTrimmed(String value, String fieldName) {
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new InvalidRequestException(fieldName + " must not be blank.");
        }
        return trimmed;
    }

    private List<String> validateModalities(List<String> values) {
        Set<String> uniqueModalities = values.stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase())
                .filter(value -> !value.isBlank())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        List<String> modalities = uniqueModalities.stream().toList();

        if (modalities.isEmpty()) {
            throw new InvalidRequestException("modalities must contain at least one value.");
        }
        if (!ALLOWED_MODALITIES.containsAll(modalities)) {
            throw new InvalidRequestException("modalities only allows text or audio.");
        }
        return modalities;
    }

    private List<String> parseModalities(String configuredModalities) {
        if (configuredModalities == null || configuredModalities.isBlank()) {
            return List.of("text");
        }

        List<String> modalities = Arrays.stream(configuredModalities.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return modalities.isEmpty() ? List.of("text") : modalities;
    }
}
