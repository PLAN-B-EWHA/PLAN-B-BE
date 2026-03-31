package myexpressionfriend_api.unity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.common.service.GeminiLlmClient;
import myexpressionfriend_api.unity.dto.UnityMissionGenerationType;
import myexpressionfriend_api.unity.dto.UnityMissionImportRequestDTO;
import myexpressionfriend_api.unity.dto.UnityMissionImportResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * LLM 기반 Unity 미션 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UnityMissionGenerationService {

    private final GeminiLlmClient geminiLlmClient;
    private final UnityMissionLlmParsingService parsingService;
    private final UnityMissionService unityMissionService;
    private final UnityMissionPromptBuilderService promptBuilderService;

    /**
     * 아동 정보로 Unity 미션 생성 후 저장
     */
    @Transactional
    public UnityMissionImportResultDTO generateAndSaveMissionsForChild(
            UUID childId, UUID userId,
            UnityMissionGenerationType generationType,
            int missionIdStart, int maxTokens, String modelName
    ) {
        String prompt = promptBuilderService.buildPrompt(childId, userId, missionIdStart, generationType);
        Set<String> allowedEmotions = promptBuilderService.resolveAllowedTargetEmotions(childId, userId);

        String rawResponse = geminiLlmClient.generate(prompt, maxTokens, modelName);
        UnityMissionImportRequestDTO requestDTO = parseSafe(rawResponse, generationType, allowedEmotions, prompt, maxTokens, modelName);
        return unityMissionService.importMissionsForChild(requestDTO, childId, LocalDate.now());
    }

    /**
     * 아동 정보로 Unity 미션 DTO만 생성 (저장 안 함, 미리보기용)
     */
    public UnityMissionImportRequestDTO generateMissionRequestForChild(
            UUID childId, UUID userId,
            UnityMissionGenerationType generationType,
            int missionIdStart, int maxTokens, String modelName
    ) {
        String prompt = promptBuilderService.buildPrompt(childId, userId, missionIdStart, generationType);
        Set<String> allowedEmotions = promptBuilderService.resolveAllowedTargetEmotions(childId, userId);

        String rawResponse = geminiLlmClient.generate(prompt, maxTokens, modelName);
        return parseSafe(rawResponse, generationType, allowedEmotions, prompt, maxTokens, modelName);
    }

    /**
     * Expression/Situation 개수를 지정해 아동 맞춤 미션 일괄 생성 및 저장
     * - 각 미션은 독립 트랜잭션으로 저장 (중간 실패해도 이전 결과 유지)
     */
    @Transactional
    public UnityMissionImportResultDTO generateBulkMissionsForChild(
            UUID childId, UUID userId,
            int expressionCount, int situationCount,
            int maxTokens, String modelName
    ) {
        List<Long> allSavedIds = new ArrayList<>();
        int missionIdCounter = 1;

        for (int i = 0; i < expressionCount; i++) {
            UnityMissionImportResultDTO result = generateAndSaveMissionsForChild(
                    childId, userId, UnityMissionGenerationType.EXPRESSION, missionIdCounter++, maxTokens, modelName);
            allSavedIds.addAll(result.getSavedIds());
        }

        for (int i = 0; i < situationCount; i++) {
            UnityMissionImportResultDTO result = generateAndSaveMissionsForChild(
                    childId, userId, UnityMissionGenerationType.SITUATION, missionIdCounter++, maxTokens, modelName);
            allSavedIds.addAll(result.getSavedIds());
        }

        return UnityMissionImportResultDTO.builder()
                .requestedCount(expressionCount + situationCount)
                .savedCount(allSavedIds.size())
                .savedIds(allSavedIds)
                .build();
    }

    // ── private ────────────────────────────────────────────────────────────────

    private UnityMissionImportRequestDTO parseSafe(
            String rawResponse, UnityMissionGenerationType generationType,
            Set<String> allowedEmotions, String prompt, int maxTokens, String modelName
    ) {
        try {
            return parsingService.parseMissionBatch(rawResponse, generationType, allowedEmotions);
        } catch (InvalidRequestException e) {
            log.warn("Unity mission parse failed. type={}, model={}, maxTokens={}, promptLen={}, responseLen={}, preview={}",
                    generationType, modelName, maxTokens,
                    prompt != null ? prompt.length() : 0,
                    rawResponse != null ? rawResponse.length() : 0,
                    abbreviate(rawResponse));
            throw e;
        }
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) return "(empty)";
        String normalized = value.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }
}
