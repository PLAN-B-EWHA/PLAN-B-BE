package myexpressionfriend_api.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import myexpressionfriend_api.report.domain.ReportType;

import java.util.UUID;

public record ReportGenerateDraftRequest(
        @NotNull
        UUID childId,

        @NotNull
        ReportType reportType,

        String title,

        @NotBlank
        String request,

        String retrievalQuery,

        String childSummary,

        String additionalContext,

        String templateKey,

        Integer topK,

        Double similarityThreshold,

        Boolean useProModel
) {
}
