package myexpressionfriend_api.unity.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.ExpressionTag;
import myexpressionfriend_api.child.dto.ChildDetailResponseDTO;
import myexpressionfriend_api.child.service.ChildQueryService;
import myexpressionfriend_api.unity.domain.UnityGameResult;
import myexpressionfriend_api.unity.dto.UnityMissionGenerationType;
import myexpressionfriend_api.unity.dto.prompt.UnityMissionPromptContextDTO;
import myexpressionfriend_api.unity.repository.UnityGameResultRepository;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Unity 미션 생성용 프롬프트 조합 서비스
 * <p>아동 정보 + 최근 게임 결과를 바탕으로 LLM 프롬프트를 구성</p>
 */
@Service
@RequiredArgsConstructor
public class UnityMissionPromptBuilderService {

    private static final String DEFAULT_CHILD_NAME       = "친구";
    private static final String DEFAULT_INTERESTS        = "일상생활";
    private static final String DEFAULT_ABC_SUMMARY      = "최근 관찰기록 없음";
    private static final String DEFAULT_AFFINITY_LEVEL   = "1";
    private static final String DEFAULT_UNITY_SUMMARY    = "최근 수행기록 없음";

    private final UnityMissionPromptTemplateService templateService;
    private final ChildQueryService childQueryService;
    private final UnityGameResultRepository unityGameResultRepository;

    /** 아동 정보를 기반으로 프롬프트를 생성 */
    public String buildPrompt(UUID childId, UUID userId, int missionIdStart, UnityMissionGenerationType generationType) {
        ChildDetailResponseDTO child = childQueryService.getChildDetail(childId, userId);
        UnityMissionPromptContextDTO context = buildContext(childId, child, missionIdStart);
        return applyContext(templateService.loadTemplate(generationType), context);
    }

    /** 아동 기준 허용 감정 문자열 집합 반환 */
    public Set<String> resolveAllowedTargetEmotions(UUID childId, UUID userId) {
        ChildDetailResponseDTO child = childQueryService.getChildDetail(childId, userId);
        return buildAllowedEmotionSet(child);
    }

    // ── private ────────────────────────────────────────────────────────────────

    private UnityMissionPromptContextDTO buildContext(UUID childId, ChildDetailResponseDTO child, int missionIdStart) {
        return UnityMissionPromptContextDTO.builder()
                .childName(defaultString(child.getName(), DEFAULT_CHILD_NAME))
                .childInterests(defaultString(child.getInterests(), DEFAULT_INTERESTS))
                .childProfileSummary(buildProfileSummary(child))
                .abcObservationSummary(DEFAULT_ABC_SUMMARY)   // 노트 도메인 미구현 → 기본값
                .characterAffinityLevel(DEFAULT_AFFINITY_LEVEL)
                .defaultAffinityLevel(DEFAULT_AFFINITY_LEVEL)
                .recentUnitySummary(buildUnitySummary(childId))
                .allowedTargetEmotions(String.join(", ", buildAllowedEmotionSet(child)))
                .missionIdStart(String.valueOf(missionIdStart))
                .missionIdNext(String.valueOf(missionIdStart + 1))
                .build();
    }

    private String applyContext(String template, UnityMissionPromptContextDTO ctx) {
        return template
                .replace("${childName}", ctx.getChildName())
                .replace("${childInterests}", ctx.getChildInterests())
                .replace("${childProfileSummary}", ctx.getChildProfileSummary())
                .replace("${abcObservationSummary}", ctx.getAbcObservationSummary())
                .replace("${characterAffinityLevel}", ctx.getCharacterAffinityLevel())
                .replace("${defaultAffinityLevel}", ctx.getDefaultAffinityLevel())
                .replace("${recentUnitySummary}", ctx.getRecentUnitySummary())
                .replace("${allowedTargetEmotions}", ctx.getAllowedTargetEmotions())
                .replace("${missionIdStart}", ctx.getMissionIdStart())
                .replace("${missionIdNext}", ctx.getMissionIdNext());
    }

    private String buildProfileSummary(ChildDetailResponseDTO child) {
        String notes            = defaultString(child.getSpecialNotes(), "특이사항 없음");
        String preferred        = joinSet(child.getPreferredExpressions());
        String difficult        = joinSet(child.getDifficultExpressions());
        String languageSkill    = child.getLanguageSkill() != null ? child.getLanguageSkill().name() : "보통";
        String sensoryProcessing = child.getSensoryProcessing() != null ? child.getSensoryProcessing().name() : "보통";
        return "특이사항: %s / 선호 표정: %s / 어려운 표정: %s / 언어 수준: %s / 감각 특성: %s"
                .formatted(notes, preferred, difficult, languageSkill, sensoryProcessing);
    }

    private String buildUnitySummary(UUID childId) {
        List<UnityGameResult> results = unityGameResultRepository
                .findTop5ByGameSession_Child_ChildIdOrderByCreatedAtDesc(childId);
        if (results.isEmpty()) {
            return DEFAULT_UNITY_SUMMARY;
        }
        return results.stream()
                .map(r -> "missionId=%d, success=%s, score=%d, retry=%d"
                        .formatted(r.getMissionId(), r.getSuccess(), r.getScore(), r.getRetryCount()))
                .collect(Collectors.joining(" | "));
    }

    private Set<String> buildAllowedEmotionSet(ChildDetailResponseDTO child) {
        Set<ExpressionTag> tags = EnumSet.noneOf(ExpressionTag.class);
        if (child.getPreferredExpressions() != null) tags.addAll(child.getPreferredExpressions());
        if (child.getDifficultExpressions() != null) tags.addAll(child.getDifficultExpressions());
        if (tags.isEmpty()) tags.addAll(EnumSet.allOf(ExpressionTag.class));

        return tags.stream()
                .map(this::toTargetEmotion)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private String toTargetEmotion(ExpressionTag tag) {
        return switch (tag) {
            case JOY     -> "Happiness";
            case SAD     -> "Sadness";
            case FEAR    -> "Fear";
            case SURPRISE -> "Surprise";
            case DISGUST -> "Disgust";
            case ANGRY   -> "Anger";
        };
    }

    private String joinSet(Set<?> values) {
        if (values == null || values.isEmpty()) return "없음";
        return values.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String defaultString(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }
}
