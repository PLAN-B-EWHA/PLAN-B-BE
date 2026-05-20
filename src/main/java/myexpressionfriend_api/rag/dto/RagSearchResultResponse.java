package myexpressionfriend_api.rag.dto;

import org.springframework.ai.document.Document;

import java.util.Map;

public record RagSearchResultResponse(
        String documentId,
        String text,
        Double score,
        Map<String, Object> metadata
) {

    public static RagSearchResultResponse from(Document document) {
        return new RagSearchResultResponse(
                document.getId(),
                document.getText(),
                document.getScore(),
                document.getMetadata()
        );
    }
}
