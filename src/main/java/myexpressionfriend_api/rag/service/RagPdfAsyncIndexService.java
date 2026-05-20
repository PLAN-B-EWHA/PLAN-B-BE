package myexpressionfriend_api.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.rag.domain.RagMetadataKeys;
import myexpressionfriend_api.rag.domain.RagSource;
import myexpressionfriend_api.rag.repository.RagSourceRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RagPdfAsyncIndexService {

    private final RagSourceRepository ragSourceRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagTextChunker ragTextChunker;

    @Async
    @Transactional
    public void indexPdf(UUID sourceId, byte[] pdfBytes) {
        RagSource source = ragSourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("RAG source not found. id=" + sourceId));

        try {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) {
                throw new IllegalStateException("VectorStore bean is not available. Enable Spring AI embedding and pgvector settings.");
            }

            List<PageText> pages = extractPages(pdfBytes);
            if (pages.isEmpty()) {
                throw new IllegalArgumentException("PDF에서 추출 가능한 텍스트를 찾을 수 없습니다.");
            }

            String rawContent = toRawContent(pages);
            List<Document> documents = toDocuments(source, pages);
            addInBatches(vectorStore, documents);

            source.updateExtractedContent(rawContent, sha256(rawContent));
            source.markIndexed(documents.size());
            log.info("RAG PDF indexed. sourceId={}, pages={}, chunks={}", sourceId, pages.size(), documents.size());
        } catch (Exception ex) {
            source.markFailed(ex.getMessage());
            log.warn("RAG PDF indexing failed. sourceId={}", sourceId, ex);
        }
    }

    private void addInBatches(VectorStore vectorStore, List<Document> documents) {
        int batchSize = 100;
        for (int start = 0; start < documents.size(); start += batchSize) {
            int end = Math.min(start + batchSize, documents.size());
            vectorStore.add(documents.subList(start, end));
        }
    }

    private List<PageText> extractPages(byte[] pdfBytes) throws java.io.IOException {
        List<PageText> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = normalize(stripper.getText(document));
                if (!text.isBlank()) {
                    pages.add(new PageText(pageNumber, text));
                }
            }
        }
        return pages;
    }

    private List<Document> toDocuments(RagSource source, List<PageText> pages) {
        List<Document> documents = new ArrayList<>();
        int chunkIndex = 0;
        for (PageText page : pages) {
            for (String chunk : ragTextChunker.split(page.text())) {
                Map<String, Object> metadata = buildMetadata(source, chunkIndex, page.pageNumber());
                documents.add(new Document(chunk, metadata));
                chunkIndex++;
            }
        }
        return documents;
    }

    private Map<String, Object> buildMetadata(RagSource source, int chunkIndex, int pageNumber) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(RagMetadataKeys.SOURCE_TYPE, source.getSourceType().name());
        metadata.put(RagMetadataKeys.USE_CASE, source.getUseCase().name());
        metadata.put(RagMetadataKeys.SOURCE_ID, source.getSourceId().toString());
        metadata.put(RagMetadataKeys.DOCUMENT_ID, source.getSourceId().toString());
        metadata.put(RagMetadataKeys.CHUNK_INDEX, chunkIndex);
        metadata.put(RagMetadataKeys.PAGE_NUMBER, pageNumber);
        metadata.put(RagMetadataKeys.ORIGINAL_FILENAME, source.getOriginalFilename() == null ? "" : source.getOriginalFilename());
        metadata.put(RagMetadataKeys.CREATED_AT, LocalDateTime.now().toString());
        metadata.put(RagMetadataKeys.CHILD_ID, source.getChild() != null
                ? source.getChild().getChildId().toString()
                : RagMetadataKeys.GLOBAL_CHILD_ID);
        metadata.put(RagMetadataKeys.USER_ID, source.getUploadedBy() != null
                ? source.getUploadedBy().getUserId().toString()
                : "");
        return metadata;
    }

    private String toRawContent(List<PageText> pages) {
        StringBuilder builder = new StringBuilder();
        for (PageText page : pages) {
            builder.append("[page ").append(page.pageNumber()).append("]\n")
                    .append(page.text()).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", ex);
        }
    }

    private record PageText(int pageNumber, String text) {
    }
}
