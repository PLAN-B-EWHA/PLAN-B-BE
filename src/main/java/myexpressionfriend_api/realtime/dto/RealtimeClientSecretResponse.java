package myexpressionfriend_api.realtime.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record RealtimeClientSecretResponse(
        String clientSecret,
        Long expiresAt,
        String model,
        JsonNode session
) {
}
