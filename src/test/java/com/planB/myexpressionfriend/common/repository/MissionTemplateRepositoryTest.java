package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("MissionTemplate Repository 테스트")
@Transactional
public class MissionTemplateRepositoryTest {

    @Autowired
    private MissionTemplateRepository templateRepository;

    @Autowired
    private TestEntityManager em;

    private MissionTemplate expressionBeginner;
    private MissionTemplate emotionIntermediate;
    private MissionTemplate communicationAdvanced;
    private MissionTemplate llmGeneratedTemplate;
    private MissionTemplate inactiveTemplate;

    @BeforeEach
    void setUp() {
        // 1. 표정 인식 - 초급
        expressionBeginner = MissionTemplate.builder()
                .title("기본 표정 따라하기")
                .description("기초 표정을 인식하고 따라하는 연습")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("거울을 보며 표정을 따라해 보세요")
                .expectedDuration(10)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(expressionBeginner);

        // 2. 감정 인식 - 중급
        emotionIntermediate = MissionTemplate.builder()
                .title("감정 상황 이해하기")
                .description("상황에 맞는 감정을 구분하는 연습")
                .category(MissionCategory.EMOTION_RECOGNITION)
                .difficulty(MissionDifficulty.INTERMEDIATE)
                .instructions("1. 상황을 읽고 감정을 말해 보세요\n2. 이유를 함께 설명해 보세요")
                .expectedDuration(15)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(emotionIntermediate);

        // 3. 소통 - 고급
        communicationAdvanced = MissionTemplate.builder()
                .title("상황별 대화 이어가기")
                .description("대화 맥락에 맞는 표현을 연습")
                .category(MissionCategory.COMMUNICATION)
                .difficulty(MissionDifficulty.ADVANCED)
                .instructions("제시된 상황에 맞는 문장을 만들어 보세요")
                .expectedDuration(20)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(communicationAdvanced);

        // 4. LLM 생성 템플릿
        llmGeneratedTemplate = MissionTemplate.builder()
                .title("AI 추천 표정 미션")
                .description("AI가 생성한 미션 설명")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("안내에 따라 수행해 보세요")
                .expectedDuration(10)
                .llmGenerated(true)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(llmGeneratedTemplate);

        // 5. 비활성 템플릿
        inactiveTemplate = MissionTemplate.builder()
                .title("비활성 템플릿")
                .description("현재 사용하지 않는 템플릿")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("사용되지 않음")
                .expectedDuration(10)
                .llmGenerated(false)
                .active(false)
                .isDeleted(false)
                .build();
        em.persist(inactiveTemplate);

        em.flush();
        em.clear();
    }

    // ============= 기본 조회 =============

    @Test
    @DisplayName("활성 템플릿은 ID로 조회된다")
    void findByIdAndActive_Active_Success() {
        Optional<MissionTemplate> result = templateRepository.findByIdAndActive(
                expressionBeginner.getTemplateId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("비활성 템플릿은 활성 조회에서 제외된다")
    void findByIdAndActive_Inactive_Empty() {
        Optional<MissionTemplate> result = templateRepository.findByIdAndActive(
                inactiveTemplate.getTemplateId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전체 활성 템플릿 목록 조회")
    void findAllActive_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> result = templateRepository.findAllActive(pageRequest);

        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent()).allMatch(MissionTemplate::getActive);
    }

    // ============= 카테고리/난이도 조회 =============

    @Test
    @DisplayName("카테고리별 활성 템플릿 조회")
    void findByCategoryAndActive_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> expressionTemplates = templateRepository.findByCategoryAndActive(
                MissionCategory.EXPRESSION,
                pageRequest
        );

        assertThat(expressionTemplates.getContent()).hasSize(2);
        assertThat(expressionTemplates.getContent())
                .allMatch(template -> template.getCategory() == MissionCategory.EXPRESSION);
    }

    @Test
    @DisplayName("난이도별 활성 템플릿 조회")
    void findByDifficultyAndActive_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> beginnerTemplates = templateRepository.findByDifficultyAndActive(
                MissionDifficulty.BEGINNER,
                pageRequest
        );

        assertThat(beginnerTemplates.getContent()).hasSize(2);
        assertThat(beginnerTemplates.getContent())
                .allMatch(template -> template.getDifficulty() == MissionDifficulty.BEGINNER);
    }

    @Test
    @DisplayName("카테고리와 난이도로 활성 템플릿 조회")
    void findByCategoryAndDifficultyAndActive_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> result = templateRepository.findByCategoryAndDifficultyAndActive(
                MissionCategory.EXPRESSION,
                MissionDifficulty.BEGINNER,
                pageRequest
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(template -> template.getCategory() == MissionCategory.EXPRESSION
                        && template.getDifficulty() == MissionDifficulty.BEGINNER);
    }

    // ============= 검색 =============

    @Test
    @DisplayName("제목 키워드로 활성 템플릿 검색")
    void searchByKeywordAndActive_Title_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> result = templateRepository.searchByKeywordAndActive(
                "표정",
                pageRequest
        );

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("설명 키워드로 활성 템플릿 검색")
    void searchByKeywordAndActive_Description_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> result = templateRepository.searchByKeywordAndActive(
                "연습",
                pageRequest
        );

        assertThat(result.getContent()).hasSize(2);
    }

    // ============= 생성 방식 조회 =============

    @Test
    @DisplayName("LLM 생성 템플릿 조회")
    void findLLMGeneratedTemplates_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> result = templateRepository.findLLMGeneratedTemplates(pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLlmGenerated()).isTrue();
    }

    @Test
    @DisplayName("수동 생성 템플릿 조회")
    void findManualTemplates_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> result = templateRepository.findManualTemplates(pageRequest);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(template -> !template.getLlmGenerated());
    }

    // ============= 통계 =============

    @Test
    @DisplayName("활성 템플릿 개수 조회")
    void countActive_Success() {
        long count = templateRepository.countActive();

        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("카테고리별 템플릿 개수 조회")
    void countByCategory_Success() {
        long expressionCount = templateRepository.countByCategory(MissionCategory.EXPRESSION);
        long emotionCount = templateRepository.countByCategory(MissionCategory.EMOTION_RECOGNITION);

        assertThat(expressionCount).isEqualTo(2);
        assertThat(emotionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("LLM 생성 템플릿 개수 조회")
    void countLLMGenerated_Success() {
        long count = templateRepository.countLLMGenerated();

        assertThat(count).isEqualTo(1);
    }

    // ============= Soft Delete =============

    @Test
    @DisplayName("삭제된 템플릿은 활성 목록에서 제외된다")
    void findAllActive_DeletedTemplate_Excluded() {
        expressionBeginner.delete();
        em.merge(expressionBeginner);
        em.flush();
        em.clear();

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MissionTemplate> result = templateRepository.findAllActive(pageRequest);

        assertThat(result.getContent()).hasSize(3);
    }

    // ============= 비활성 조회 =============

    @Test
    @DisplayName("비활성 템플릿 목록 조회")
    void findInactiveTemplates_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<MissionTemplate> result = templateRepository.findInactiveTemplates(pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActive()).isFalse();
    }
}