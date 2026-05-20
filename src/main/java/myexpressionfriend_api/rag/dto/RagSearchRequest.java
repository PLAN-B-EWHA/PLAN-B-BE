package myexpressionfriend_api.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import myexpressionfriend_api.rag.domain.RagUseCase;

import java.util.UUID;

public record RagSearchRequest(
        @NotBlank
        String query,

        @NotNull
        RagUseCase useCase,

        UUID childId,

        Integer topK,

        Double similarityThreshold
) {
}
