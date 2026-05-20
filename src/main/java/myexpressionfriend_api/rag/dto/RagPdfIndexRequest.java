package myexpressionfriend_api.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import myexpressionfriend_api.rag.domain.RagSourceType;
import myexpressionfriend_api.rag.domain.RagUseCase;

import java.util.UUID;

public record RagPdfIndexRequest(
        @NotBlank
        String title,

        @NotNull
        RagSourceType sourceType,

        @NotNull
        RagUseCase useCase,

        UUID childId
) {
}
