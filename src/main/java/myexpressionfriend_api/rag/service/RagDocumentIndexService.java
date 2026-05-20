package myexpressionfriend_api.rag.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.rag.domain.RagMetadataKeys;
import myexpressionfriend_api.rag.domain.RagSource;
import myexpressionfriend_api.rag.domain.RagSourceType;
import myexpressionfriend_api.rag.domain.RagUseCase;
import myexpressionfriend_api.rag.repository.RagSourceRepository;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagDocumentIndexService {

    private final RagSourceRepository ragSourceRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagTextChunker ragTextChunker;

    @Transactional(readOnly = true)
    public RagSource getSource(UUID sourceId) {
        return ragSourceRepository.findById(sourceId)
                .orElseThrow(() -> new EntityNotFoundException("RAG source not found. id=" + sourceId));
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public RagSource indexText(IndexTextCommand command) {
        validate(command);

        RagSource source = RagSource.builder()
                .sourceType(command.sourceType())
                .useCase(command.useCase())
                .title(command.title().trim())
                .originalFilename(command.originalFilename())
                .contentType(command.contentType())
                .contentHash(sha256(command.content()))
                .rawContent(command.content())
                .child(command.child())
                .uploadedBy(command.uploadedBy())
                .build();

        RagSource saved = ragSourceRepository.save(source);
        saved.markIndexing();

        try {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) {
                throw new IllegalStateException("VectorStore bean is not available. Enable Spring AI embedding and pgvector settings.");
            }

            List<Document> documents = toDocuments(saved, command.content());
            vectorStore.add(documents);
            saved.markIndexed(documents.size());
            return saved;
        } catch (RuntimeException ex) {
            saved.markFailed(ex.getMessage());
            throw ex;
        }
    }

    private List<Document> toDocuments(RagSource source, String content) {
        List<String> chunks = ragTextChunker.split(content);
        List<Document> documents = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = buildMetadata(source, i);
            documents.add(new Document(chunks.get(i), metadata));
        }

        return documents;
    }

    private Map<String, Object> buildMetadata(RagSource source, int chunkIndex) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(RagMetadataKeys.SOURCE_TYPE, source.getSourceType().name());
        metadata.put(RagMetadataKeys.USE_CASE, source.getUseCase().name());
        metadata.put(RagMetadataKeys.SOURCE_ID, source.getSourceId().toString());
        metadata.put(RagMetadataKeys.DOCUMENT_ID, source.getSourceId().toString());
        metadata.put(RagMetadataKeys.CHUNK_INDEX, chunkIndex);
        metadata.put(RagMetadataKeys.CREATED_AT, LocalDateTime.now().toString());
        metadata.put(RagMetadataKeys.CHILD_ID, source.getChild() != null
                ? source.getChild().getChildId().toString()
                : RagMetadataKeys.GLOBAL_CHILD_ID);
        metadata.put(RagMetadataKeys.USER_ID, source.getUploadedBy() != null
                ? source.getUploadedBy().getUserId().toString()
                : "");
        return metadata;
    }

    private void validate(IndexTextCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("RAG index command is required.");
        }
        if (command.sourceType() == null) {
            throw new IllegalArgumentException("RAG source type is required.");
        }
        if (command.useCase() == null) {
            throw new IllegalArgumentException("RAG use case is required.");
        }
        if (command.title() == null || command.title().isBlank()) {
            throw new IllegalArgumentException("RAG source title is required.");
        }
        if (command.content() == null || command.content().isBlank()) {
            throw new IllegalArgumentException("RAG source content is required.");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", ex);
        }
    }

    public record IndexTextCommand(
            RagSourceType sourceType,
            RagUseCase useCase,
            String title,
            String originalFilename,
            String contentType,
            String content,
            Child child,
            User uploadedBy
    ) {
    }
}
