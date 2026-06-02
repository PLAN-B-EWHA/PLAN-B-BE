package myexpressionfriend_api.realtime.dto;

import java.util.List;

public record RealtimeAdminConfigResponse(
        boolean enabled,
        String baseUrl,
        String model,
        String voice,
        List<String> modalities,
        String instructions
) {
}
