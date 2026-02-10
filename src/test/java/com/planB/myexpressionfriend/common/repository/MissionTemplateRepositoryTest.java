package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import org.assertj.core.api.Assertions;
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

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("MissionTemplateRepository 테스트")
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
        // 1. 표정 - 초급
        expressionBeginner = MissionTemplate.builder()
                .title("기쁨 표정 짓기")
                .description("거울을 보며 기쁜 표정을 연습합니다")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("1. 거울 앞에 섭니다\n2. 웃는 얼굴을 만듭니다\n3. 5초간 유지합니다")
                .expectedDuration(10)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(expressionBeginner);

        // 2. 감정인식 - 중급
        emotionIntermediate = MissionTemplate.builder()
                .title("슬픔 표정 인식하기")
                .description("그림 카드를 보고 슬픈 표정을 찾습니다")
                .category(MissionCategory.EMOTION_RECOGNITION)
                .difficulty(MissionDifficulty.INTERMEDIATE)
                .instructions("1. 감정 카드를 준비합니다\n2. 슬픈 표정을 찾습니다")
                .expectedDuration(15)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(emotionIntermediate);

        // 3. 소통 - 고급
        communicationAdvanced = MissionTemplate.builder()
                .title("상황에 맞는 인사하기")
                .description("다양한 상황에서 적절한 인사를 연습합니다")
                .category(MissionCategory.COMMUNICATION)
                .difficulty(MissionDifficulty.ADVANCED)
                .instructions("1. 상황 카드를 봅니다\n2. 알맞은 인사를 선택합니다")
                .expectedDuration(20)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(communicationAdvanced);

        // 4. LLM 생성 템플릿
        llmGeneratedTemplate = MissionTemplate.builder()
                .title("LLM이 만든 미션")
                .description("AI가 생성한 표정 훈련 미션입니다")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("AI 생성 지침")
                .expectedDuration(10)
                .llmGenerated(true)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(llmGeneratedTemplate);

        // 5. 비활성화된 템플릿
        inactiveTemplate = MissionTemplate.builder()
                .title("비활성화된 미션")
                .description("사용하지 않는 미션입니다")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("비활성화됨")
                .expectedDuration(10)
                .llmGenerated(false)
                .active(false)
                .isDeleted(false)
                .build();
        em.persist(inactiveTemplate);

        em.flush();
        em.clear();
    }

    // ============= 기본 조회 테스트 =============

    @Test
    @DisplayName("활성화된 템플릿을 ID로 조회할 수 있다")
    void findByIdAndActive_Active_Success() {

        // when
        Optional<MissionTemplate> result = templateRepository.findByIdAndActive(
                expressionBeginner.getTemplateId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("기쁨 표정 짓기");
    }

    @Test
    @DisplayName("비활성화된 템플릿은 조회되지 않는다")
    void findByIdAndActive_Inactive_Empty() {

        // when
        Optional<MissionTemplate> result = templateRepository.findByIdAndActive(
                inactiveTemplate.getTemplateId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("모든 활성화된 템플릿을 페이징하여 조회할 수 있다")
    void findAllActive_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.findAllActive(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(4); // 비활성화 1개 제외
        assertThat(result.getContent())
                .allMatch(template -> template.getActive());
    }

    // ============= 카테고리별 조회 테스트 =============

    @Test
    @DisplayName("카테고리별로 템플릿을 조회할 수 있다")
    void findByCategoryAndActive_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> expressionTemplates = templateRepository.findByCategoryAndActive(
                MissionCategory.EXPRESSION,
                pageRequest
        );

        // then
        assertThat(expressionTemplates.getContent()).hasSize(2);
        assertThat(expressionTemplates.getContent())
                .allMatch(template -> template.getCategory() == MissionCategory.EXPRESSION);
    }

    @Test
    @DisplayName("난이도별로 템플릿을 조회할 수 있다")
    void findByDifficultyAndActive_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> beginnerTemplates = templateRepository.findByDifficultyAndActive(
                MissionDifficulty.BEGINNER,
                pageRequest
        );

        // then
        assertThat(beginnerTemplates.getContent()).hasSize(2);
        assertThat(beginnerTemplates.getContent())
                .allMatch(template -> template.getDifficulty() == MissionDifficulty.BEGINNER);
    }

    @Test
    @DisplayName("카테고리와 난이도를 함께 필터링할 수 있다")
    void findByCategoryAndDifficultyAndActive_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.findByCategoryAndDifficultyAndActive(
                MissionCategory.EXPRESSION,
                MissionDifficulty.BEGINNER,
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(template ->
                        template.getCategory() == MissionCategory.EXPRESSION &&
                                template.getDifficulty() == MissionDifficulty.BEGINNER
                );
    }

    // ============= 검색 테스트 =============

    @Test
    @DisplayName("키워드로 템플릿을 검색할 수 있다 (제목)")
    void searchByKeywordAndActive_Title_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.searchByKeywordAndActive(
                "표정",
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(3); // 제목이나 설명에 "표정" 포함
    }

    @Test
    @DisplayName("키워드로 템플릿을 검색할 수 있다 (설명)")
    void searchByKeywordAndActive_Description_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.searchByKeywordAndActive(
                "카드",
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(2); // 감정인식, 소통 미션
    }

    // ============= LLM 관련 테스트 =============

    @Test
    @DisplayName("LLM 생성 템플릿만 조회할 수 있다")
    void findLLMGeneratedTemplates_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.findLLMGeneratedTemplates(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLlmGenerated()).isTrue();
    }

    @Test
    @DisplayName("수동 생성 템플릿만 조회할 수 있다")
    void findManualTemplates_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.findManualTemplates(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .allMatch(template -> !template.getLlmGenerated());
    }

    // ============= 통계 테스트 =============

    @Test
    @DisplayName("활성화된 템플릿 총 개수를 조회할 수 있다")
    void countActive_Success() {
        // when
        long count = templateRepository.countActive();

        // then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("카테고리별 템플릿 개수를 조회할 수 있다")
    void countByCategory_Success() {
        // when
        long expressionCount = templateRepository.countByCategory(MissionCategory.EXPRESSION);
        long emotionCount = templateRepository.countByCategory(MissionCategory.EMOTION_RECOGNITION);

        // then
        assertThat(expressionCount).isEqualTo(2);
        assertThat(emotionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("LLM 생성 템플릿 개수를 조회할 수 있다")
    void countLLMGenerated_Success() {
        // when
        long count = templateRepository.countLLMGenerated();

        // then
        assertThat(count).isEqualTo(1);
    }

    // ============= Soft Delete 테스트 =============

    @Test
    @DisplayName("삭제된 템플릿은 조회되지 않는다")
    void findAllActive_DeletedTemplate_Excluded() {
        // given
        expressionBeginner.delete();
        em.merge(expressionBeginner);
        em.flush();
        em.clear();

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.findAllActive(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(3); // 1개 삭제됨
    }

    // ============= 관리자용 메서드 테스트 =============

    @Test
    @DisplayName("비활성화된 템플릿만 조회할 수 있다")
    void findInactiveTemplates_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.findInactiveTemplates(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActive()).isFalse();
    }

}
