package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.dto.child.ChildDetailDTO;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.service.ChildService;
import com.planB.myexpressionfriend.unity.domain.UnityGameResult;
import com.planB.myexpressionfriend.unity.dto.UnityMissionGenerationType;
import com.planB.myexpressionfriend.unity.dto.prompt.UnityMissionPromptContextDTO;
import com.planB.myexpressionfriend.unity.repository.UnityGameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Unity 미션 프롬프트 조합 서비스
 */
@Service
@RequiredArgsConstructor
public class UnityMissionPromptBuilderService {

    private static final String DEFAULT_CHILD_NAME = "친구";
    private static final String DEFAULT_INTERESTS = "일상생활";
    private static final String DEFAULT_ABC_SUMMARY = "최근 관찰기록 없음";
    private static final String DEFAULT_AFFINITY_LEVEL = "1";
    private static final String DEFAULT_UNITY_SUMMARY = "최근 수행기록 없음";

    private final UnityMissionPromptTemplateService templateService;
    private final ChildService childService;
    private final ChildNoteRepository childNoteRepository;
    private final UnityGameResultRepository unityGameResultRepository;

    /**
     * 아동 정보를 기반으로 프롬프트를 생성합니다.
     */
    public String buildPrompt(
            UUID childId,
            UUID userId,
            int missionIdStart,
            UnityMissionGenerationType generationType
    ) {
        ChildDetailDTO child = childService.getChildDetail(childId, userId);
        UnityMissionPromptContextDTO context = buildContext(childId, child, missionIdStart);
        return applyContext(templateService.loadTemplate(generationType), context);
    }

    /**
     * 아동 기준 허용 감정 문자열을 반환합니다.
     */
    public Set<String> resolveAllowedTargetEmotions(UUID childId, UUID userId) {
        ChildDetailDTO child = childService.getChildDetail(childId, userId);
        return buildAllowedEmotionSet(child);
    }

    /**
     * 프롬프트 컨텍스트를 생성합니다.
     */
    private UnityMissionPromptContextDTO buildContext(
            UUID childId,
            ChildDetailDTO child,
            int missionIdStart
    ) {
        return UnityMissionPromptContextDTO.builder()
                .childName(defaultString(child.getName(), DEFAULT_CHILD_NAME))
                .childInterests(defaultString(child.getInterests(), DEFAULT_INTERESTS))
                .childProfileSummary(buildProfileSummary(child))
                .abcObservationSummary(buildObservationSummary(childId))
                .characterAffinityLevel(DEFAULT_AFFINITY_LEVEL)
                .defaultAffinityLevel(DEFAULT_AFFINITY_LEVEL)
                .recentUnitySummary(buildUnitySummary(childId))
                .allowedTargetEmotions(String.join(", ", buildAllowedEmotionSet(child)))
                .missionIdStart(String.valueOf(missionIdStart))
                .missionIdNext(String.valueOf(missionIdStart + 1))
                .build();
    }

    /**
     * 템플릿에 값을 치환합니다.
     */
    private String applyContext(String template, UnityMissionPromptContextDTO context) {
        return template
                .replace("${childName}", context.getChildName())
                .replace("${childInterests}", context.getChildInterests())
                .replace("${childProfileSummary}", context.getChildProfileSummary())
                .replace("${abcObservationSummary}", context.getAbcObservationSummary())
                .replace("${characterAffinityLevel}", context.getCharacterAffinityLevel())
                .replace("${defaultAffinityLevel}", context.getDefaultAffinityLevel())
                .replace("${recentUnitySummary}", context.getRecentUnitySummary())
                .replace("${allowedTargetEmotions}", context.getAllowedTargetEmotions())
                .replace("${missionIdStart}", context.getMissionIdStart())
                .replace("${missionIdNext}", context.getMissionIdNext());
    }

    /**
     * 아동 정적 프로필 요약을 생성합니다.
     */
    private String buildProfileSummary(ChildDetailDTO child) {
        String notes = defaultString(child.getSpecialNotes(), "특이사항 없음");
        String preferred = joinSet(child.getPreferredExpressions());
        String difficult = joinSet(child.getDifficultExpressions());
        String languageSkill = child.getLanguageSkill() != null ? child.getLanguageSkill().name() : "보통";
        String sensoryProcessing = child.getSensoryProcessing() != null
                ? child.getSensoryProcessing().name()
                : "보통";

        return String.format(
                "특이사항: %s / 선호 표정: %s / 어려운 표정: %s / 언어 수준: %s / 감각 특성: %s",
                notes,
                preferred,
                difficult,
                languageSkill,
                sensoryProcessing
        );
    }

    /**
     * 최근 노트 기반 관찰 요약을 생성합니다.
     */
    private String buildObservationSummary(UUID childId) {
        List<ChildNote> recentNotes = childNoteRepository.findAllByChildId(childId).stream()
                .limit(3)
                .toList();

        if (recentNotes.isEmpty()) {
            return DEFAULT_ABC_SUMMARY;
        }

        return recentNotes.stream()
                .map(note -> {
                    String title = defaultString(note.getTitle(), "제목 없음");
                    return title + ": " + abbreviate(note.getContent(), 80);
                })
                .collect(Collectors.joining(" | "));
    }

    /**
     * 최근 Unity 결과 요약을 생성합니다.
     */
    private String buildUnitySummary(UUID childId) {
        List<UnityGameResult> results = unityGameResultRepository
                .findTop5ByGameSession_Child_ChildIdOrderByCreatedAtDesc(childId);

        if (results.isEmpty()) {
            return DEFAULT_UNITY_SUMMARY;
        }

        return results.stream()
                .map(result -> String.format(
                        "missionId=%d, success=%s, score=%d, retry=%d",
                        result.getMissionId(),
                        result.getSuccess(),
                        result.getScore(),
                        result.getRetryCount()
                ))
                .collect(Collectors.joining(" | "));
    }

    private Set<String> buildAllowedEmotionSet(ChildDetailDTO child) {
        Set<ExpressionTag> tags = EnumSet.noneOf(ExpressionTag.class);

        if (child.getPreferredExpressions() != null) {
            tags.addAll(child.getPreferredExpressions());
        }
        if (child.getDifficultExpressions() != null) {
            tags.addAll(child.getDifficultExpressions());
        }
        if (tags.isEmpty()) {
            tags.addAll(EnumSet.allOf(ExpressionTag.class));
        }

        return tags.stream()
                .map(this::toTargetEmotion)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private String toTargetEmotion(ExpressionTag tag) {
        return switch (tag) {
            case JOY -> "Happiness";
            case SAD -> "Sadness";
            case FEAR -> "Fear";
            case SURPRISE -> "Surprise";
            case DISGUST -> "Disgust";
            case ANGRY -> "Anger";
        };
    }

    private String joinSet(Set<?> values) {
        if (values == null || values.isEmpty()) {
            return "없음";
        }
        return values.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "내용 없음";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    private String defaultString(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
