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
@DisplayName("MissionTemplateRepository ?뚯뒪??)
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
        // 1. ?쒖젙 - 초급
        expressionBeginner = MissionTemplate.builder()
                .title("湲곗겏 ?쒖젙 吏볤린")
                .description("嫄곗슱??蹂대ŉ 湲곗걶 ?쒖젙???곗뒿?⑸땲??)
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("1. 嫄곗슱 ?욎뿉 ??땲??n2. ?껊뒗 ?쇨뎬??留뚮벊?덈떎\n3. 5珥덇컙 ?좎??⑸땲??)
                .expectedDuration(10)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(expressionBeginner);

        // 2. 媛먯젙?몄떇 - 중급
        emotionIntermediate = MissionTemplate.builder()
                .title("?ы뵒 표정 인식?섍린")
                .description("洹몃┝ 移대뱶瑜?蹂닿퀬 ?ы뵂 ?쒖젙??李얠뒿?덈떎")
                .category(MissionCategory.EMOTION_RECOGNITION)
                .difficulty(MissionDifficulty.INTERMEDIATE)
                .instructions("1. 媛먯젙 移대뱶瑜?以鍮꾪빀?덈떎\n2. ?ы뵂 ?쒖젙??李얠뒿?덈떎")
                .expectedDuration(15)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(emotionIntermediate);

        // 3. 소통 - 고급
        communicationAdvanced = MissionTemplate.builder()
                .title("?곹솴??留욌뒗 ?몄궗?섍린")
                .description("?ㅼ뼇???곹솴?먯꽌 ?곸젅???몄궗瑜??곗뒿?⑸땲??)
                .category(MissionCategory.COMMUNICATION)
                .difficulty(MissionDifficulty.ADVANCED)
                .instructions("1. ?곹솴 移대뱶瑜?遊낅땲??n2. ?뚮쭪? ?몄궗瑜??좏깮?⑸땲??)
                .expectedDuration(20)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(communicationAdvanced);

        // 4. LLM ?앹꽦 ?쒗뵆由?
        llmGeneratedTemplate = MissionTemplate.builder()
                .title("LLM??留뚮뱺 誘몄뀡")
                .description("AI媛 ?앹꽦???쒖젙 ?덈젴 誘몄뀡?낅땲??)
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("AI ?앹꽦 吏移?)
                .expectedDuration(10)
                .llmGenerated(true)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(llmGeneratedTemplate);

        // 5. 鍮꾪솢?깊솕???쒗뵆由?
        inactiveTemplate = MissionTemplate.builder()
                .title("鍮꾪솢?깊솕??誘몄뀡")
                .description("?ъ슜?섏? ?딅뒗 誘몄뀡?낅땲??)
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("鍮꾪솢?깊솕??)
                .expectedDuration(10)
                .llmGenerated(false)
                .active(false)
                .isDeleted(false)
                .build();
        em.persist(inactiveTemplate);

        em.flush();
        em.clear();
    }

    // ============= 湲곕낯 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?쒖꽦?붾맂 ?쒗뵆由우쓣 ID濡?議고쉶?????덈떎")
    void findByIdAndActive_Active_Success() {

        // when
        Optional<MissionTemplate> result = templateRepository.findByIdAndActive(
                expressionBeginner.getTemplateId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("湲곗겏 ?쒖젙 吏볤린");
    }

    @Test
    @DisplayName("鍮꾪솢?깊솕???쒗뵆由우? 議고쉶?섏? ?딅뒗??)
    void findByIdAndActive_Inactive_Empty() {

        // when
        Optional<MissionTemplate> result = templateRepository.findByIdAndActive(
                inactiveTemplate.getTemplateId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("紐⑤뱺 ?쒖꽦?붾맂 ?쒗뵆由우쓣 ?섏씠吏뺥븯??議고쉶?????덈떎")
    void findAllActive_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.findAllActive(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(4); // 鍮꾪솢?깊솕 1媛??쒖쇅
        assertThat(result.getContent())
                .allMatch(template -> template.getActive());
    }

    // ============= 移댄뀒怨좊━蹂?議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("移댄뀒怨좊━蹂꾨줈 ?쒗뵆由우쓣 議고쉶?????덈떎")
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
    @DisplayName("?쒖씠?꾨퀎濡??쒗뵆由우쓣 議고쉶?????덈떎")
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
    @DisplayName("移댄뀒怨좊━? ?쒖씠?꾨? ?④퍡 ?꾪꽣留곹븷 ???덈떎")
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

    // ============= 寃???뚯뒪??=============

    @Test
    @DisplayName("?ㅼ썙?쒕줈 ?쒗뵆由우쓣 寃?됲븷 ???덈떎 (?쒕ぉ)")
    void searchByKeywordAndActive_Title_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.searchByKeywordAndActive(
                "?쒖젙",
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(3); // ?쒕ぉ?대굹 ?ㅻ챸??"?쒖젙" ?ы븿
    }

    @Test
    @DisplayName("?ㅼ썙?쒕줈 ?쒗뵆由우쓣 寃?됲븷 ???덈떎 (?ㅻ챸)")
    void searchByKeywordAndActive_Description_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<MissionTemplate> result = templateRepository.searchByKeywordAndActive(
                "移대뱶",
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(2); // 媛먯젙?몄떇, 소통 誘몄뀡
    }

    // ============= LLM 愿???뚯뒪??=============

    @Test
    @DisplayName("LLM ?앹꽦 ?쒗뵆由용쭔 議고쉶?????덈떎")
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
    @DisplayName("?섎룞 ?앹꽦 ?쒗뵆由용쭔 議고쉶?????덈떎")
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

    // ============= ?듦퀎 ?뚯뒪??=============

    @Test
    @DisplayName("?쒖꽦?붾맂 ?쒗뵆由?珥?媛쒖닔瑜?議고쉶?????덈떎")
    void countActive_Success() {
        // when
        long count = templateRepository.countActive();

        // then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("移댄뀒怨좊━蹂??쒗뵆由?媛쒖닔瑜?議고쉶?????덈떎")
    void countByCategory_Success() {
        // when
        long expressionCount = templateRepository.countByCategory(MissionCategory.EXPRESSION);
        long emotionCount = templateRepository.countByCategory(MissionCategory.EMOTION_RECOGNITION);

        // then
        assertThat(expressionCount).isEqualTo(2);
        assertThat(emotionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("LLM ?앹꽦 ?쒗뵆由?媛쒖닔瑜?議고쉶?????덈떎")
    void countLLMGenerated_Success() {
        // when
        long count = templateRepository.countLLMGenerated();

        // then
        assertThat(count).isEqualTo(1);
    }

    // ============= Soft Delete ?뚯뒪??=============

    @Test
    @DisplayName("??젣???쒗뵆由우? 議고쉶?섏? ?딅뒗??)
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
        assertThat(result.getContent()).hasSize(3); // 1媛???젣??
    }

    // ============= 愿由ъ옄??硫붿꽌???뚯뒪??=============

    @Test
    @DisplayName("鍮꾪솢?깊솕???쒗뵆由용쭔 議고쉶?????덈떎")
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
