package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
import com.planB.myexpressionfriend.common.domain.child.LanguageSkill;
import com.planB.myexpressionfriend.common.domain.child.SensoryProcessing;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.dto.child.ChildDetailDTO;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.service.ChildService;
import com.planB.myexpressionfriend.unity.domain.UnityGameResult;
import com.planB.myexpressionfriend.unity.dto.UnityMissionGenerationType;
import com.planB.myexpressionfriend.unity.repository.UnityGameResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnityMissionPromptBuilderServiceTest {

    @Mock
    private UnityMissionPromptTemplateService templateService;
    @Mock
    private ChildService childService;
    @Mock
    private ChildNoteRepository childNoteRepository;
    @Mock
    private UnityGameResultRepository unityGameResultRepository;

    @InjectMocks
    private UnityMissionPromptBuilderService promptBuilderService;

    @Test
    @DisplayName("아동 정보와 최근 기록이 프롬프트 템플릿에 치환된다")
    void buildPrompt_withChildData_replacesTemplateValues() {
        UUID childId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ChildDetailDTO child = ChildDetailDTO.builder()
                .name("민수")
                .interests("공룡 놀이")
                .specialNotes("큰 소리에 민감함")
                .preferredExpressions(Set.of(ExpressionTag.JOY))
                .difficultExpressions(Set.of(ExpressionTag.SAD))
                .languageSkill(LanguageSkill.SHORT_SENTENCE)
                .sensoryProcessing(SensoryProcessing.AUDITORY_SENSITIVITY)
                .build();

        ChildNote note = ChildNote.builder()
                .title("관찰 기록")
                .content("친구가 울자 민수가 잠시 멈추고 교사를 바라보았습니다.")
                .build();

        UnityGameResult result = UnityGameResult.builder()
                .missionId(7)
                .success(true)
                .score(95)
                .retryCount(1)
                .build();

        when(templateService.loadTemplate(UnityMissionGenerationType.EXPRESSION)).thenReturn("""
                이름=${childName}
                관심사=${childInterests}
                프로필=${childProfileSummary}
                관찰=${abcObservationSummary}
                친밀도=${characterAffinityLevel}
                수행=${recentUnitySummary}
                허용감정=${allowedTargetEmotions}
                시작=${missionIdStart}
                다음=${missionIdNext}
                """);
        when(childService.getChildDetail(childId, userId)).thenReturn(child);
        when(childNoteRepository.findAllByChildId(childId)).thenReturn(List.of(note));
        when(unityGameResultRepository.findTop5ByGameSession_Child_ChildIdOrderByCreatedAtDesc(childId))
                .thenReturn(List.of(result));

        String prompt = promptBuilderService.buildPrompt(
                childId,
                userId,
                10,
                UnityMissionGenerationType.EXPRESSION
        );

        assertTrue(prompt.contains("이름=민수"));
        assertTrue(prompt.contains("관심사=공룡 놀이"));
        assertTrue(prompt.contains("관찰=관찰 기록:"));
        assertTrue(prompt.contains("missionId=7, success=true, score=95, retry=1"));
        assertTrue(prompt.contains("허용감정=Happiness, Sadness"));
        assertTrue(prompt.contains("시작=10"));
        assertTrue(prompt.contains("다음=11"));
    }

    @Test
    @DisplayName("아동 정보가 비어 있으면 기본값으로 프롬프트를 생성한다")
    void buildPrompt_withMissingData_usesDefaults() {
        UUID childId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ChildDetailDTO child = ChildDetailDTO.builder().build();

        when(templateService.loadTemplate(UnityMissionGenerationType.SITUATION)).thenReturn("""
                이름=${childName}
                관심사=${childInterests}
                관찰=${abcObservationSummary}
                친밀도=${characterAffinityLevel}
                수행=${recentUnitySummary}
                허용감정=${allowedTargetEmotions}
                """);
        when(childService.getChildDetail(childId, userId)).thenReturn(child);
        when(childNoteRepository.findAllByChildId(childId)).thenReturn(List.of());
        when(unityGameResultRepository.findTop5ByGameSession_Child_ChildIdOrderByCreatedAtDesc(childId))
                .thenReturn(List.of());

        String prompt = promptBuilderService.buildPrompt(
                childId,
                userId,
                1,
                UnityMissionGenerationType.SITUATION
        );

        assertTrue(prompt.contains("이름=OO이"));
        assertTrue(prompt.contains("관심사=일상생활"));
        assertTrue(prompt.contains("관찰=최근 관찰기록 없음"));
        assertTrue(prompt.contains("친밀도=1"));
        assertTrue(prompt.contains("수행=최근 수행기록 없음"));
        assertTrue(prompt.contains("허용감정=Happiness"));
        assertTrue(prompt.contains("Anger"));
    }
}
