package myexpressionfriend_api.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.rag.domain.RagSource;
import myexpressionfriend_api.rag.dto.RagPdfIndexRequest;
import myexpressionfriend_api.rag.repository.RagSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagPdfIndexService {

    private static final long MAX_PDF_SIZE_BYTES = 100L * 1024L * 1024L;

    private final RagSourceRepository ragSourceRepository;
    private final RagPdfAsyncIndexService ragPdfAsyncIndexService;

    public RagSource uploadPdf(RagPdfIndexRequest request, MultipartFile file, Child child, User uploadedBy) {
        validate(request, file);
        byte[] pdfBytes = readBytes(file);

        RagSource source = RagSource.builder()
                .sourceType(request.sourceType())
                .useCase(request.useCase())
                .title(request.title().trim())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType() == null ? "application/pdf" : file.getContentType())
                .child(child)
                .uploadedBy(uploadedBy)
                .build();

        RagSource saved = ragSourceRepository.save(source);
        saved.markIndexing();
        saved = ragSourceRepository.save(saved);
        log.info("RAG PDF indexing submitted. sourceId={}, filename={}, size={} bytes",
                saved.getSourceId(), file.getOriginalFilename(), file.getSize());
        ragPdfAsyncIndexService.indexPdf(saved.getSourceId(), pdfBytes);
        return saved;
    }

    private void validate(RagPdfIndexRequest request, MultipartFile file) {
        if (request == null) {
            throw new IllegalArgumentException("RAG PDF index request is required.");
        }
        if (request.sourceType() == null) {
            throw new IllegalArgumentException("RAG source type is required.");
        }
        if (request.useCase() == null) {
            throw new IllegalArgumentException("RAG use case is required.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("RAG source title is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required.");
        }
        if (file.getSize() > MAX_PDF_SIZE_BYTES) {
            throw new IllegalArgumentException("PDF file must be 100MB or smaller.");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!filename.endsWith(".pdf") && !contentType.contains("pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded PDF file.", ex);
        }
    }
}
