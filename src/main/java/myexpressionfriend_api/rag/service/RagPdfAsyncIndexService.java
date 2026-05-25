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

    @Async("taskExecutor")
    @Transactional
    public void indexPdf(UUID sourceId, byte[] pdfBytes) {
        log.info("RAG PDF indexing started. sourceId={}, size={} bytes", sourceId, pdfBytes.length);
        RagSource source = ragSourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("RAG source not found. id=" + sourceId));

        try {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) {
                throw new IllegalStateException("VectorStore bean is not available. Enable Spring AI embedding and pgvector settings.");
            }

            List<PageText> pages = extractPages(sourceId, pdfBytes);
            log.info("RAG PDF pages extracted. sourceId={}, pagesWithText={}", sourceId, pages.size());
            if (pages.isEmpty()) {
                throw new IllegalArgumentException("PDF에서 추출 가능한 텍스트를 찾을 수 없습니다.");
            }

            String rawContent = toRawContent(pages);
            List<Document> documents = toDocuments(source, pages);
            log.info("RAG PDF chunks created. sourceId={}, chunks={}", sourceId, documents.size());
            addInBatches(sourceId, vectorStore, documents);

            source.updateExtractedContent(rawContent, sha256(rawContent));
            source.markIndexed(documents.size());
            log.info("RAG PDF indexed. sourceId={}, pages={}, chunks={}", sourceId, pages.size(), documents.size());
        } catch (Exception ex) {
            source.markFailed(ex.getMessage());
            log.warn("RAG PDF indexing failed. sourceId={}", sourceId, ex);
        }
    }

    private void addInBatches(UUID sourceId, VectorStore vectorStore, List<Document> documents) {
        int batchSize = 10;
        for (int start = 0; start < documents.size(); start += batchSize) {
            int end = Math.min(start + batchSize, documents.size());
            log.info("RAG PDF vector batch adding. sourceId={}, batch={}..{} of {}",
                    sourceId, start + 1, end, documents.size());
            vectorStore.add(documents.subList(start, end));
            log.info("RAG PDF vector batch added. sourceId={}, indexedChunks={} of {}",
                    sourceId, end, documents.size());
        }
    }

    private List<PageText> extractPages(UUID sourceId, byte[] pdfBytes) throws java.io.IOException {
        List<PageText> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();
            log.info("RAG PDF loaded. sourceId={}, totalPages={}", sourceId, pageCount);
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setShouldSeparateByBeads(false);
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = normalize(stripper.getText(document));
                if (isIndexablePageText(text)) {
                    pages.add(new PageText(pageNumber, text));
                }
                if (pageNumber % 25 == 0 || pageNumber == pageCount) {
                    log.info("RAG PDF page extraction progress. sourceId={}, page={} of {}, pagesWithText={}",
                            sourceId, pageNumber, pageCount, pages.size());
                }
            }
        }
        return pages;
    }

    private boolean isIndexablePageText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.toLowerCase();
        if (normalized.contains("this page intentionally left blank")) {
            return false;
        }

        String alphanumericOnly = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
        return alphanumericOnly.length() >= 80;
    }

    private List<Document> toDocuments(RagSource source, List<PageText> pages) {
        List<Document> documents = new ArrayList<>();
        int chunkIndex = 0;
        for (PageText page : pages) {
            for (String chunk : ragTextChunker.split(page.text())) {
                if (!isIndexableChunk(chunk)) {
                    log.debug("RAG PDF chunk skipped by quality filter. sourceId={}, page={}",
                            source.getSourceId(), page.pageNumber());
                    continue;
                }
                Map<String, Object> metadata = buildMetadata(source, chunkIndex, page.pageNumber());
                documents.add(new Document(chunk, metadata));
                chunkIndex++;
            }
        }
        return documents;
    }

    private boolean isIndexableChunk(String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return false;
        }

        String normalized = chunk.trim();
        String lettersAndDigits = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
        if (lettersAndDigits.length() < 120) {
            return false;
        }

        List<String> tokens = List.of(normalized.split("\\s+"));
        if (tokens.size() < 20) {
            return false;
        }

        long shortTokens = tokens.stream()
                .map(token -> token.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", ""))
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() <= 2)
                .count();
        double shortTokenRatio = (double) shortTokens / Math.max(tokens.size(), 1);
        if (shortTokenRatio > 0.45) {
            return false;
        }

        String[] lines = normalized.split("\\n");
        long noisyLines = 0;
        for (String line : lines) {
            String compact = line.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
            if (!compact.isBlank() && compact.length() <= 3 && line.length() >= 8) {
                noisyLines++;
            }
        }
        double noisyLineRatio = (double) noisyLines / Math.max(lines.length, 1);
        return noisyLineRatio <= 0.35;
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
