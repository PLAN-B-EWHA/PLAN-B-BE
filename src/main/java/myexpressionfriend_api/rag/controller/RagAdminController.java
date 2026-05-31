package myexpressionfriend_api.rag.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.dto.UserDTO;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.rag.domain.RagSource;
import myexpressionfriend_api.rag.dto.RagGenerateRequest;
import myexpressionfriend_api.rag.dto.RagGenerateResponse;
import myexpressionfriend_api.rag.dto.RagPdfIndexRequest;
import myexpressionfriend_api.rag.dto.RagSearchRequest;
import myexpressionfriend_api.rag.dto.RagSearchResultResponse;
import myexpressionfriend_api.rag.dto.RagSourceResponse;
import myexpressionfriend_api.rag.dto.RagTextIndexRequest;
import myexpressionfriend_api.rag.service.RagDocumentIndexService;
import myexpressionfriend_api.rag.service.RagGenerationService;
import myexpressionfriend_api.rag.service.RagPdfIndexService;
import myexpressionfriend_api.rag.service.RagRetrievalService;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 - RAG", description = "RAG 자료 인덱싱, 검색, 생성 테스트 API")
public class RagAdminController {

    private static final long MAX_TEXT_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Charset MS949 = Charset.forName("MS949");

    private final RagDocumentIndexService ragDocumentIndexService;
    private final RagRetrievalService ragRetrievalService;
    private final RagGenerationService ragGenerationService;
    private final RagPdfIndexService ragPdfIndexService;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/sources/text")
    @Operation(summary = "텍스트 자료 RAG 인덱싱", description = "일반 텍스트 자료를 청크로 나누어 pgvector에 저장합니다. 리포트, 오프라인 미션, 시나리오 생성에서 검색 자료로 사용할 수 있습니다.")
    public ResponseEntity<ApiResponse<RagSourceResponse>> indexText(
            @AuthenticationPrincipal UserDTO adminUser,
            @Valid @RequestBody RagTextIndexRequest request
    ) {
        Child child = request.childId() == null
                ? null
                : childRepository.findById(request.childId())
                .orElseThrow(() -> new EntityNotFoundException("Child not found. id=" + request.childId()));

        User uploadedBy = userRepository.findById(adminUser.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found. id=" + adminUser.getUserId()));

        RagSource source = ragDocumentIndexService.indexText(
                new RagDocumentIndexService.IndexTextCommand(
                        request.sourceType(),
                        request.useCase(),
                        request.title(),
                        request.originalFilename(),
                        request.contentType() != null && !request.contentType().isBlank()
                                ? request.contentType()
                                : "text/plain",
                        request.content(),
                        child,
                        uploadedBy
                )
        );

        return ResponseEntity.ok(ApiResponse.success("RAG text source indexed.", RagSourceResponse.from(source)));
    }

    @PostMapping(value = "/sources/text-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "텍스트 파일 RAG 인덱싱", description = "txt, md, csv 같은 텍스트 파일을 업로드해 pgvector에 저장합니다.")
    public ResponseEntity<ApiResponse<RagSourceResponse>> indexTextFile(
            @AuthenticationPrincipal UserDTO adminUser,
            @RequestPart("request") String requestJson,
            @RequestPart("file") MultipartFile file
    ) {
        RagPdfIndexRequest request = parsePdfIndexRequest(requestJson);
        validateTextFile(file);

        Child child = request.childId() == null
                ? null
                : childRepository.findById(request.childId())
                .orElseThrow(() -> new EntityNotFoundException("Child not found. id=" + request.childId()));

        User uploadedBy = userRepository.findById(adminUser.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found. id=" + adminUser.getUserId()));

        RagSource source = ragDocumentIndexService.indexText(
                new RagDocumentIndexService.IndexTextCommand(
                        request.sourceType(),
                        request.useCase(),
                        request.title(),
                        file.getOriginalFilename(),
                        file.getContentType() == null || file.getContentType().isBlank()
                                ? "text/plain"
                                : file.getContentType(),
                        readTextFile(file),
                        child,
                        uploadedBy
                )
        );

        return ResponseEntity.ok(ApiResponse.success("RAG text file source indexed.", RagSourceResponse.from(source)));
    }

    @PostMapping(value = "/sources/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "PDF 자료 RAG 비동기 인덱싱", description = "PDF 자료를 먼저 등록한 뒤, 백그라운드에서 텍스트를 추출해 pgvector에 저장합니다.")
    public ResponseEntity<ApiResponse<RagSourceResponse>> indexPdf(
            @AuthenticationPrincipal UserDTO adminUser,
            @RequestPart("request") String requestJson,
            @RequestPart("file") MultipartFile file
    ) {
        RagPdfIndexRequest request = parsePdfIndexRequest(requestJson);
        Child child = request.childId() == null
                ? null
                : childRepository.findById(request.childId())
                .orElseThrow(() -> new EntityNotFoundException("Child not found. id=" + request.childId()));

        User uploadedBy = userRepository.findById(adminUser.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found. id=" + adminUser.getUserId()));

        RagSource source = ragPdfIndexService.uploadPdf(request, file, child, uploadedBy);
        return ResponseEntity.ok(ApiResponse.success("RAG PDF source accepted. Indexing runs in the background.", RagSourceResponse.from(source)));
    }

    private RagPdfIndexRequest parsePdfIndexRequest(String requestJson) {
        try {
            RagPdfIndexRequest request = objectMapper.readValue(requestJson, RagPdfIndexRequest.class);
            validatePdfIndexRequest(request);
            return request;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid PDF index request JSON.", e);
        }
    }

    private void validatePdfIndexRequest(RagPdfIndexRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("title is required.");
        }
        if (request.sourceType() == null) {
            throw new IllegalArgumentException("sourceType is required.");
        }
        if (request.useCase() == null) {
            throw new IllegalArgumentException("useCase is required.");
        }
    }

    private void validateTextFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Text file is required.");
        }
        if (file.getSize() > MAX_TEXT_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Text file must be 5MB or smaller.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean supportedExtension = filename.endsWith(".txt")
                || filename.endsWith(".md")
                || filename.endsWith(".csv")
                || filename.endsWith(".text");
        boolean supportedContentType = contentType.startsWith("text/")
                || contentType.contains("csv")
                || contentType.equals("application/octet-stream");
        if (!supportedExtension && !supportedContentType) {
            throw new IllegalArgumentException("Only text files are supported.");
        }
    }

    private String readTextFile(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded text file.", ex);
        }

        try {
            return decodeStrict(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException ex) {
            return new String(bytes, MS949);
        }
    }

    private String decodeStrict(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
                .replace("\uFEFF", "");
    }

    @GetMapping("/sources/{sourceId}")
    @Operation(summary = "RAG 자료 상태 조회", description = "비동기 인덱싱 상태, 청크 수, 실패 메시지 등을 조회합니다.")
    public ResponseEntity<ApiResponse<RagSourceResponse>> getSource(@PathVariable UUID sourceId) {
        RagSource source = ragDocumentIndexService.getSource(sourceId);
        return ResponseEntity.ok(ApiResponse.success(RagSourceResponse.from(source)));
    }

    @PostMapping("/search")
    @Operation(summary = "RAG 컨텍스트 검색", description = "사용 목적과 아동 범위를 기준으로 인덱싱된 RAG 청크를 검색합니다.")
    public ResponseEntity<ApiResponse<List<RagSearchResultResponse>>> search(
            @Valid @RequestBody RagSearchRequest request
    ) {
        if (request.childId() != null && !childRepository.existsById(request.childId())) {
            throw new EntityNotFoundException("Child not found. id=" + request.childId());
        }

        List<Document> documents = ragRetrievalService.retrieve(
                new RagRetrievalService.RetrieveCommand(
                        request.query(),
                        request.useCase(),
                        request.childId(),
                        request.topK(),
                        request.similarityThreshold()
                )
        );

        List<RagSearchResultResponse> result = documents.stream()
                .map(RagSearchResultResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("RAG 검색이 완료되었습니다.", result));
    }

    @PostMapping("/generate/report")
    @Operation(summary = "RAG 기반 리포트 생성", description = "리포트 생성용 자료를 검색한 뒤 기존 LLM 클라이언트로 리포트를 생성합니다.")
    public ResponseEntity<ApiResponse<RagGenerateResponse>> generateReport(
            @Valid @RequestBody RagGenerateRequest request
    ) {
        validateChildExists(request.childId());
        RagGenerateResponse response = ragGenerationService.generateReport(request);
        return ResponseEntity.ok(ApiResponse.success("RAG 기반 리포트가 생성되었습니다.", response));
    }

    @PostMapping("/debug/generate/report")
    @Operation(summary = "RAG 기반 리포트 생성 디버그", description = "리포트를 생성하고, 관리자 검토를 위해 검색된 RAG 컨텍스트와 최종 프롬프트를 함께 반환합니다.")
    public ResponseEntity<ApiResponse<RagGenerateResponse>> generateReportDebug(
            @Valid @RequestBody RagGenerateRequest request
    ) {
        validateChildExists(request.childId());
        RagGenerateResponse response = ragGenerationService.generateReport(withDebugContext(request));
        return ResponseEntity.ok(ApiResponse.success("RAG 기반 리포트가 디버그 정보와 함께 생성되었습니다.", response));
    }

    @PostMapping("/generate/offline-mission")
    @Operation(summary = "RAG 기반 오프라인 미션 생성", description = "오프라인 미션 생성용 자료를 검색한 뒤 기존 LLM 클라이언트로 미션을 생성합니다.")
    public ResponseEntity<ApiResponse<RagGenerateResponse>> generateOfflineMission(
            @Valid @RequestBody RagGenerateRequest request
    ) {
        validateChildExists(request.childId());
        RagGenerateResponse response = ragGenerationService.generateOfflineMission(request);
        return ResponseEntity.ok(ApiResponse.success("RAG 기반 오프라인 미션이 생성되었습니다.", response));
    }

    @PostMapping("/debug/generate/offline-mission")
    @Operation(summary = "RAG 기반 오프라인 미션 생성 디버그", description = "오프라인 미션을 생성하고, 관리자 검토를 위해 검색된 RAG 컨텍스트와 최종 프롬프트를 함께 반환합니다.")
    public ResponseEntity<ApiResponse<RagGenerateResponse>> generateOfflineMissionDebug(
            @Valid @RequestBody RagGenerateRequest request
    ) {
        validateChildExists(request.childId());
        RagGenerateResponse response = ragGenerationService.generateOfflineMission(withDebugContext(request));
        return ResponseEntity.ok(ApiResponse.success("RAG 기반 오프라인 미션이 디버그 정보와 함께 생성되었습니다.", response));
    }

    @PostMapping("/generate/scenario")
    @Operation(summary = "RAG 기반 시나리오 생성", description = "시나리오 생성용 자료를 검색한 뒤 기존 LLM 클라이언트로 시나리오 JSON 초안을 생성합니다.")
    public ResponseEntity<ApiResponse<RagGenerateResponse>> generateScenario(
            @Valid @RequestBody RagGenerateRequest request
    ) {
        validateChildExists(request.childId());
        RagGenerateResponse response = ragGenerationService.generateScenario(request);
        return ResponseEntity.ok(ApiResponse.success("RAG 기반 시나리오가 생성되었습니다.", response));
    }

    @PostMapping("/debug/generate/scenario")
    @Operation(summary = "RAG 기반 시나리오 생성 디버그", description = "시나리오를 생성하고, 관리자 검토를 위해 검색된 RAG 컨텍스트와 최종 프롬프트를 함께 반환합니다.")
    public ResponseEntity<ApiResponse<RagGenerateResponse>> generateScenarioDebug(
            @Valid @RequestBody RagGenerateRequest request
    ) {
        validateChildExists(request.childId());
        RagGenerateResponse response = ragGenerationService.generateScenario(withDebugContext(request));
        return ResponseEntity.ok(ApiResponse.success("RAG 기반 시나리오가 디버그 정보와 함께 생성되었습니다.", response));
    }

    private void validateChildExists(java.util.UUID childId) {
        if (childId != null && !childRepository.existsById(childId)) {
            throw new EntityNotFoundException("Child not found. id=" + childId);
        }
    }

    private RagGenerateRequest withDebugContext(RagGenerateRequest request) {
        return new RagGenerateRequest(
                request.childId(),
                request.request(),
                request.retrievalQuery(),
                request.childSummary(),
                request.additionalContext(),
                request.templateKey(),
                request.topK(),
                request.similarityThreshold(),
                request.useProModel(),
                true,
                request.think()
        );
    }
}
