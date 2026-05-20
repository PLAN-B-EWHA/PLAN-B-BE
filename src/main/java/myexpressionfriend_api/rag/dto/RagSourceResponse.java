package myexpressionfriend_api.rag.dto;

import myexpressionfriend_api.rag.domain.RagSource;
import myexpressionfriend_api.rag.domain.RagSourceStatus;
import myexpressionfriend_api.rag.domain.RagSourceType;
import myexpressionfriend_api.rag.domain.RagUseCase;

import java.time.LocalDateTime;
import java.util.UUID;

public record RagSourceResponse(
        UUID sourceId,
        RagSourceType sourceType,
        RagUseCase useCase,
        String title,
        String originalFilename,
        String contentType,
        UUID childId,
        UUID uploadedByUserId,
        RagSourceStatus status,
        int chunkCount,
        String failureMessage,
        LocalDateTime indexedAt,
        LocalDateTime createdAt
) {

    public static RagSourceResponse from(RagSource source) {
        return new RagSourceResponse(
                source.getSourceId(),
                source.getSourceType(),
                source.getUseCase(),
                source.getTitle(),
                source.getOriginalFilename(),
                source.getContentType(),
                source.getChild() != null ? source.getChild().getChildId() : null,
                source.getUploadedBy() != null ? source.getUploadedBy().getUserId() : null,
                source.getStatus(),
                source.getChunkCount(),
                source.getFailureMessage(),
                source.getIndexedAt(),
                source.getCreatedAt()
        );
    }
}
