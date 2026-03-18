package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.common.service.ReportLlmClient;
import com.planB.myexpressionfriend.common.exception.InvalidRequestException;
import com.planB.myexpressionfriend.unity.dto.UnityMissionGenerationType;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * LLM Unity 미션 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UnityMissionGenerationService {

    private final ReportLlmClient reportLlmClient;
    private final UnityMissionLlmParsingService unityMissionLlmParsingService;
    private final UnityMissionService unityMissionService;
    private final UnityMissionPromptBuilderService unityMissionPromptBuilderService;

    /**
     * 프롬프트로 Unity 미션을 생성하고 저장합니다.
     */
    @Transactional
    public UnityMissionImportResultDTO generateAndSaveMissions(
            String prompt,
            UnityMissionGenerationType generationType,
            Set<String> allowedTargetEmotions,
            int maxTokens,
            String modelName
    ) {
        String rawResponse = reportLlmClient.generateReport(prompt, maxTokens, modelName);
        UnityMissionImportRequestDTO requestDTO = parseMissionResponse(
                rawResponse,
                generationType,
                allowedTargetEmotions,
                prompt,
                maxTokens,
                modelName
        );

        return unityMissionService.importMissions(requestDTO);
    }

    /**
     * 프롬프트로 Unity 미션 DTO만 생성합니다.
     */
    public UnityMissionImportRequestDTO generateMissionRequest(
            String prompt,
            UnityMissionGenerationType generationType,
            Set<String> allowedTargetEmotions,
            int maxTokens,
            String modelName
    ) {
        String rawResponse = reportLlmClient.generateReport(prompt, maxTokens, modelName);
        return parseMissionResponse(rawResponse, generationType, allowedTargetEmotions, prompt, maxTokens, modelName);
    }

    /**
     * 아동 정보로 Unity 미션을 생성하고 저장합니다.
     */
    @Transactional
    public UnityMissionImportResultDTO generateAndSaveMissionsForChild(
            UUID childId,
            UUID userId,
            UnityMissionGenerationType generationType,
            int missionIdStart,
            int maxTokens,
            String modelName
    ) {
        String prompt = unityMissionPromptBuilderService.buildPrompt(
                childId,
                userId,
                missionIdStart,
                generationType
        );
        Set<String> allowedTargetEmotions =
                unityMissionPromptBuilderService.resolveAllowedTargetEmotions(childId, userId);

        String rawResponse = reportLlmClient.generateReport(prompt, maxTokens, modelName);
        UnityMissionImportRequestDTO requestDTO = parseMissionResponse(
                rawResponse,
                generationType,
                allowedTargetEmotions,
                prompt,
                maxTokens,
                modelName
        );
        return unityMissionService.importMissionsForChild(requestDTO, childId, LocalDate.now());
    }

    /**
     * Expression/Situation 개수를 지정해 아동 맞춤 미션을 일괄 생성하고 저장합니다.
     * 각 미션은 독립 트랜잭션으로 저장되므로 중간에 실패해도 이전 결과는 유지됩니다.
     */
    @Transactional
    public UnityMissionImportResultDTO generateBulkMissionsForChild(
            UUID childId,
            UUID userId,
            int expressionCount,
            int situationCount,
            int maxTokens,
            String modelName
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

    /**
     * 아동 정보로 Unity 미션 DTO를 생성합니다.
     */
    public UnityMissionImportRequestDTO generateMissionRequestForChild(
            UUID childId,
            UUID userId,
            UnityMissionGenerationType generationType,
            int missionIdStart,
            int maxTokens,
            String modelName
    ) {
        String prompt = unityMissionPromptBuilderService.buildPrompt(
                childId,
                userId,
                missionIdStart,
                generationType
        );
        Set<String> allowedTargetEmotions =
                unityMissionPromptBuilderService.resolveAllowedTargetEmotions(childId, userId);
        return generateMissionRequest(prompt, generationType, allowedTargetEmotions, maxTokens, modelName);
    }

    /**
     * LLM 응답을 파싱하고 실패 로그를 남깁니다.
     */
    private UnityMissionImportRequestDTO parseMissionResponse(
            String rawResponse,
            UnityMissionGenerationType generationType,
            Set<String> allowedTargetEmotions,
            String prompt,
            int maxTokens,
            String modelName
    ) {
        try {
            return unityMissionLlmParsingService.parseMissionBatch(rawResponse, generationType, allowedTargetEmotions);
        } catch (InvalidRequestException e) {
            log.warn(
                    "Unity mission parse failed. type={}, model={}, maxTokens={}, promptLength={}, responseLength={}, responsePreview={}",
                    generationType,
                    modelName,
                    maxTokens,
                    prompt != null ? prompt.length() : 0,
                    rawResponse != null ? rawResponse.length() : 0,
                    abbreviate(rawResponse)
            );
            throw e;
        }
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "(empty)";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500) + "...";
    }
}
