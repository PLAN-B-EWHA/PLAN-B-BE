package myexpressionfriend_api.rag.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.config.LlmProperties;
import myexpressionfriend_api.common.service.LlmGenerateOptions;
import myexpressionfriend_api.common.service.LlmTextClient;
import myexpressionfriend_api.rag.domain.RagUseCase;
import myexpressionfriend_api.rag.dto.RagGenerateRequest;
import myexpressionfriend_api.rag.dto.RagGenerateResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RagGenerationService {

    private final RagRetrievalService ragRetrievalService;
    private final RagPromptBuilder ragPromptBuilder;
    private final LlmTextClient llmTextClient;
    private final LlmProperties llmProperties;

    public RagGenerateResponse generateReport(RagGenerateRequest request) {
        return generate(RagUseCase.REPORT_GENERATION, request);
    }

    public RagGenerateResponse generateOfflineMission(RagGenerateRequest request) {
        return generate(RagUseCase.OFFLINE_MISSION_GENERATION, request);
    }

    public RagGenerateResponse generateScenario(RagGenerateRequest request) {
        return generate(RagUseCase.SCENARIO_GENERATION, request);
    }

    private RagGenerateResponse generate(RagUseCase useCase, RagGenerateRequest request) {
        validate(request);

        String retrievalQuery = request.retrievalQuery() == null || request.retrievalQuery().isBlank()
                ? request.request()
                : request.retrievalQuery().trim();

        String ragContext = ragRetrievalService.retrieveContext(
                new RagRetrievalService.RetrieveCommand(
                        retrievalQuery,
                        useCase,
                        request.childId(),
                        request.topK(),
                        request.similarityThreshold()
                )
        );

        String templateKey = ragPromptBuilder.normalizeTemplateKey(useCase, request.templateKey());
        Map<String, String> promptVariables = new HashMap<>();
        promptVariables.put("rag_context", ragContext);
        promptVariables.put("child_summary", request.childSummary());
        promptVariables.put("additional_context", request.additionalContext());
        promptVariables.put("user_request", request.request());
        String prompt = ragPromptBuilder.build(templateKey, promptVariables);

        String model = Boolean.TRUE.equals(request.useProModel())
                ? llmProperties.getModelPro()
                : llmProperties.getModelFlash();

        Optional<String> generated = llmTextClient.generateText(
                model,
                prompt,
                new LlmGenerateOptions(request.think()));
        if (generated.isEmpty()) {
            throw new IllegalStateException("LLM returned empty response. Check llm.enabled, base-url, api-key, and model settings.");
        }

        boolean includeDebugContext = Boolean.TRUE.equals(request.includeDebugContext());
        return new RagGenerateResponse(
                useCase,
                request.childId(),
                model,
                templateKey,
                retrievalQuery,
                generated.get(),
                includeDebugContext ? ragContext : null,
                includeDebugContext ? prompt : null
        );
    }

    private void validate(RagGenerateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RAG generation request is required.");
        }
        if (request.request() == null || request.request().isBlank()) {
            throw new IllegalArgumentException("RAG generation user request is required.");
        }
    }
}
