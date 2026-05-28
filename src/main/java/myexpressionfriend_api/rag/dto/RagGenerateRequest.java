package myexpressionfriend_api.rag.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record RagGenerateRequest(
        UUID childId,

        @NotBlank
        String request,

        String retrievalQuery,

        String childSummary,

        String additionalContext,

        String templateKey,

        Integer topK,

        Double similarityThreshold,

        Boolean useProModel,

        Boolean includeDebugContext,

        String think
) {
}
