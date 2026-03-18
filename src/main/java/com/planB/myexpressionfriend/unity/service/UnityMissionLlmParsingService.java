package com.planB.myexpressionfriend.unity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.exception.InvalidRequestException;
import com.planB.myexpressionfriend.unity.dto.UnityMissionGenerationType;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionItemDTO;
import com.planB.myexpressionfriend.unity.dto.llm.UnityLlmMissionBatchDTO;
import com.planB.myexpressionfriend.unity.dto.llm.UnityLlmMissionDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM Unity 미션 파싱 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnityMissionLlmParsingService {

    private static final Set<String> DEFAULT_ALLOWED_EMOTIONS = Set.of(
            "Anger",
            "Disgust",
            "Fear",
            "Happiness",
            "Sadness",
            "Surprise"
    );

    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * LLM JSON 응답을 저장용 DTO로 변환합니다.
     */
    public UnityMissionImportRequestDTO parseMissionBatch(
            String rawJson,
            UnityMissionGenerationType generationType,
            Set<String> allowedTargetEmotions
    ) {
        String normalizedJson = normalizeJson(rawJson);
        UnityLlmMissionBatchDTO batch = readBatch(normalizedJson);
        validateBean(batch);
        validateMissionRules(batch, generationType, sanitizeAllowedEmotions(allowedTargetEmotions));

        return UnityMissionImportRequestDTO.builder()
                .missions(batch.getMissions().stream()
                        .map(this::toMissionItem)
                        .toList())
                .build();
    }

    /**
     * 코드 블록을 제거합니다.
     */
    private String normalizeJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new InvalidRequestException("LLM 응답이 비어 있습니다.");
        }

        String trimmed = rawJson.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFenceStart = trimmed.lastIndexOf("```");
            if (firstLineEnd > -1 && lastFenceStart > firstLineEnd) {
                trimmed = trimmed.substring(firstLineEnd + 1, lastFenceStart).trim();
            }
        }
        return trimmed;
    }

    /**
     * JSON 문자열을 DTO로 읽습니다.
     */
    private UnityLlmMissionBatchDTO readBatch(String normalizedJson) {
        try {
            return objectMapper.readValue(normalizedJson, UnityLlmMissionBatchDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("LLM JSON parse failed: {}", e.getOriginalMessage());
            throw new InvalidRequestException("LLM 응답 JSON 파싱에 실패했습니다: " + e.getOriginalMessage());
        }
    }

    /**
     * Bean Validation을 적용합니다.
     */
    private void validateBean(UnityLlmMissionBatchDTO batch) {
        Set<ConstraintViolation<UnityLlmMissionBatchDTO>> violations = validator.validate(batch);
        if (!violations.isEmpty()) {
            ConstraintViolation<UnityLlmMissionBatchDTO> violation = violations.iterator().next();
            String fieldPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "unknown";
            throw new InvalidRequestException(
                    "LLM 미션 형식이 올바르지 않습니다: %s - %s".formatted(fieldPath, violation.getMessage())
            );
        }
    }

    /**
     * 타입별 미션 개수와 공통 규칙을 검증합니다.
     */
    private void validateMissionRules(
            UnityLlmMissionBatchDTO batch,
            UnityMissionGenerationType generationType,
            Set<String> allowedTargetEmotions
    ) {
        long expressionCount = batch.getMissions().stream()
                .filter(mission -> "Expression".equals(mission.getMissionTypeString()))
                .count();
        long situationCount = batch.getMissions().stream()
                .filter(mission -> "Situation".equals(mission.getMissionTypeString()))
                .count();

        if (generationType == UnityMissionGenerationType.EXPRESSION) {
            if (expressionCount != 1) {
                throw new InvalidRequestException("표정 미션은 정확히 1개여야 합니다.");
            }
            if (situationCount != 0) {
                throw new InvalidRequestException("표정 미션 생성에는 상황 미션이 포함되면 안 됩니다.");
            }
        }

        if (generationType == UnityMissionGenerationType.SITUATION) {
            if (situationCount != 1) {
                throw new InvalidRequestException("상황 미션은 정확히 1개여야 합니다.");
            }
            if (expressionCount != 0) {
                throw new InvalidRequestException("상황 미션 생성에는 표정 미션이 포함되면 안 됩니다.");
            }
        }

        boolean hasInvalidEmotion = batch.getMissions().stream()
                .map(UnityLlmMissionDTO::getTargetEmotionString)
                .anyMatch(emotion -> !allowedTargetEmotions.contains(emotion));

        if (hasInvalidEmotion) {
            throw new InvalidRequestException(
                    "targetEmotionString은 아동의 expressionTag 범위 안에서만 선택할 수 있습니다. allowed="
                            + allowedTargetEmotions
            );
        }

        boolean hasInvalidKeyword = batch.getMissions().stream()
                .map(UnityLlmMissionDTO::getTargetKeyword)
                .anyMatch(keyword -> keyword == null || !keyword.matches("^[A-Za-z][A-Za-z0-9]*$"));

        if (hasInvalidKeyword) {
            throw new InvalidRequestException("targetKeyword는 영어와 숫자만 사용할 수 있습니다.");
        }

        long distinctMissionIds = batch.getMissions().stream()
                .map(UnityLlmMissionDTO::getMissionId)
                .distinct()
                .count();

        if (distinctMissionIds != batch.getMissions().size()) {
            throw new InvalidRequestException("missionId는 중복될 수 없습니다.");
        }
    }

    private Set<String> sanitizeAllowedEmotions(Set<String> allowedTargetEmotions) {
        if (allowedTargetEmotions == null || allowedTargetEmotions.isEmpty()) {
            return DEFAULT_ALLOWED_EMOTIONS;
        }
        return allowedTargetEmotions.stream()
                .filter(DEFAULT_ALLOWED_EMOTIONS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 저장용 DTO로 변환합니다.
     */
    private UnityMissionItemDTO toMissionItem(UnityLlmMissionDTO mission) {
        return UnityMissionItemDTO.builder()
                .missionId(mission.getMissionId())
                .missionName(mission.getMissionName())
                .missionTypeString(mission.getMissionTypeString())
                .targetKeyword(mission.getTargetKeyword())
                .targetEmotionString(mission.getTargetEmotionString())
                .expressionData(mission.getExpressionData() != null
                        ? objectMapper.valueToTree(mission.getExpressionData())
                        : null)
                .situationData(mission.getSituationData() != null
                        ? objectMapper.valueToTree(mission.getSituationData())
                        : null)
                .build();
    }
}
