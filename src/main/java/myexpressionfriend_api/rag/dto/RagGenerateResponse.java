package myexpressionfriend_api.rag.dto;

import myexpressionfriend_api.rag.domain.RagUseCase;

import java.util.UUID;

public record RagGenerateResponse(
        RagUseCase useCase,
        UUID childId,
        String model,
        String templateKey,
        String retrievalQuery,
        String generatedText,
        String ragContext,
        String prompt
) {
}
