package myexpressionfriend_api.report.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportUpdateRequest(
        @NotBlank
        String title,

        @NotBlank
        String content
) {
}
