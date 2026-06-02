package myexpressionfriend_api.realtime.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record RealtimeAdminConfigUpdateRequest(
        Boolean enabled,
        @Size(max = 255)
        String baseUrl,
        @Size(max = 100)
        String model,
        @Size(max = 50)
        String voice,
        @Size(max = 2)
        List<String> modalities,
        @Size(max = 4000)
        String instructions
) {
}
