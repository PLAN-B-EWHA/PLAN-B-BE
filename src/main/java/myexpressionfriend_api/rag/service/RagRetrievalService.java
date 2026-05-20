package myexpressionfriend_api.rag.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.rag.domain.RagMetadataKeys;
import myexpressionfriend_api.rag.domain.RagUseCase;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.0d;

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public List<Document> retrieve(RetrieveCommand command) {
        validate(command);

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("VectorStore bean is not available. Enable Spring AI embedding and pgvector settings.");
        }

        SearchRequest request = SearchRequest.builder()
                .query(command.query().trim())
                .topK(resolveTopK(command.topK()))
                .similarityThreshold(resolveSimilarityThreshold(command.similarityThreshold()))
                .filterExpression(buildFilterExpression(command.useCase(), command.childId()))
                .build();

        return vectorStore.similaritySearch(request);
    }

    public String retrieveContext(RetrieveCommand command) {
        List<Document> documents = retrieve(command);
        if (documents.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            builder.append("[RAG Context ")
                    .append(i + 1)
                    .append("]\n");
            appendMetadata(builder, document);
            builder.append(document.getText()).append("\n\n");
        }
        return builder.toString().trim();
    }

    private Filter.Expression buildFilterExpression(RagUseCase useCase, UUID childId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op useCaseFilter = useCase == RagUseCase.GENERAL_REFERENCE
                ? builder.eq(RagMetadataKeys.USE_CASE, RagUseCase.GENERAL_REFERENCE.name())
                : builder.or(
                builder.eq(RagMetadataKeys.USE_CASE, useCase.name()),
                builder.eq(RagMetadataKeys.USE_CASE, RagUseCase.GENERAL_REFERENCE.name())
        );

        FilterExpressionBuilder.Op childFilter = childId == null
                ? builder.eq(RagMetadataKeys.CHILD_ID, RagMetadataKeys.GLOBAL_CHILD_ID)
                : builder.or(
                builder.eq(RagMetadataKeys.CHILD_ID, RagMetadataKeys.GLOBAL_CHILD_ID),
                builder.eq(RagMetadataKeys.CHILD_ID, childId.toString())
        );

        return builder.and(useCaseFilter, childFilter).build();
    }

    private void appendMetadata(StringBuilder builder, Document document) {
        Object sourceType = document.getMetadata().get(RagMetadataKeys.SOURCE_TYPE);
        Object sourceId = document.getMetadata().get(RagMetadataKeys.SOURCE_ID);
        Object chunkIndex = document.getMetadata().get(RagMetadataKeys.CHUNK_INDEX);

        builder.append("sourceType=")
                .append(sourceType != null ? sourceType : "")
                .append(", sourceId=")
                .append(sourceId != null ? sourceId : "")
                .append(", chunkIndex=")
                .append(chunkIndex != null ? chunkIndex : "")
                .append(", score=")
                .append(document.getScore() != null ? document.getScore() : "")
                .append("\n");
    }

    private int resolveTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.max(1, Math.min(topK, MAX_TOP_K));
    }

    private double resolveSimilarityThreshold(Double similarityThreshold) {
        if (similarityThreshold == null) {
            return DEFAULT_SIMILARITY_THRESHOLD;
        }
        return Math.max(0.0d, Math.min(similarityThreshold, 1.0d));
    }

    private void validate(RetrieveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("RAG retrieve command is required.");
        }
        if (command.query() == null || command.query().isBlank()) {
            throw new IllegalArgumentException("RAG search query is required.");
        }
        if (command.useCase() == null) {
            throw new IllegalArgumentException("RAG use case is required.");
        }
    }

    public record RetrieveCommand(
            String query,
            RagUseCase useCase,
            UUID childId,
            Integer topK,
            Double similarityThreshold
    ) {
    }
}
