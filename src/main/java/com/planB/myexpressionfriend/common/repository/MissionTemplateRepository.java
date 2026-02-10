package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * MissionTemplate Repository
 *
 * 주요 기능:
 * - 활성화된 템플릿 조회
 * - 카테고리/난이도별 필터링
 * - LLM 생성 템플릿 관리
 */
@Repository
public interface MissionTemplateRepository extends JpaRepository<MissionTemplate, UUID> {

    // ============= 기본 조회 =============

    /**
     * 템플릿 ID로 조회 (활성화된 것만)
     *
     * @param templateId 템플릿 ID
     * @return Optional<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.templateId = :templateId
        AND t.active = true
        """)
    Optional<MissionTemplate> findByIdAndActive(@Param("templateId") UUID templateId);

    /**
     * 모든 활성화된 템플릿 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.active = true
        ORDER BY t.createdAt DESC
        """)
    Page<MissionTemplate> findAllActive(Pageable pageable);

    // ============= 카테고리별 조회 =============

    /**
     * 카테고리별 템플릿 조회 (활성화된 것만, 페이징)
     *
     * @param category 카테고리
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.category = :category
        AND t.active = true
        ORDER BY t.createdAt DESC
        """)
    Page<MissionTemplate> findByCategoryAndActive(
            @Param("category") MissionCategory category,
            Pageable pageable
    );

    /**
     * 난이도별 템플릿 조회 (활성화된 것만, 페이징)
     *
     * @param difficulty 난이도
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.difficulty = :difficulty
        AND t.active = true
        ORDER BY t.createdAt DESC
        """)
    Page<MissionTemplate> findByDifficultyAndActive(
            @Param("difficulty") MissionDifficulty difficulty,
            Pageable pageable
    );

    /**
     * 카테고리 + 난이도로 템플릿 조회 (활성화된 것만, 페이징)
     *
     * @param category 카테고리
     * @param difficulty 난이도
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.category = :category
        AND t.difficulty = :difficulty
        AND t.active = true
        ORDER BY t.createdAt DESC
        """)
    Page<MissionTemplate> findByCategoryAndDifficultyAndActive(
            @Param("category") MissionCategory category,
            @Param("difficulty") MissionDifficulty difficulty,
            Pageable pageable
    );

    // ============= 검색 =============

    /**
     * 키워드 검색 (제목 + 설명, 활성화된 것만, 페이징)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
    SELECT t FROM MissionTemplate t
    WHERE (
        t.title LIKE %:keyword% 
        OR t.description LIKE %:keyword% 
        OR t.instructions LIKE %:keyword%
    )
    AND t.active = true
    AND t.isDeleted = false
    ORDER BY t.createdAt DESC
    """)
    Page<MissionTemplate> searchByKeywordAndActive(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ============= LLM 관련 =============

    /**
     * LLM 생성 템플릿만 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.llmGenerated = true
        AND t.active = true
        ORDER BY t.createdAt DESC
        """)
    Page<MissionTemplate> findLLMGeneratedTemplates(Pageable pageable);

    /**
     * 수동 생성 템플릿만 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.llmGenerated = false
        AND t.active = true
        ORDER BY t.createdAt DESC
        """)
    Page<MissionTemplate> findManualTemplates(Pageable pageable);

    // ============= 통계 =============

    /**
     * 활성화된 템플릿 총 개수
     *
     * @return 템플릿 개수
     */
    @Query("""
        SELECT COUNT(t) FROM MissionTemplate t
        WHERE t.active = true
        """)
    long countActive();

    /**
     * 카테고리별 템플릿 개수 (활성화된 것만)
     *
     * @param category 카테고리
     * @return 템플릿 개수
     */
    @Query("""
        SELECT COUNT(t) FROM MissionTemplate t
        WHERE t.category = :category
        AND t.active = true
        """)
    long countByCategory(@Param("category") MissionCategory category);

    /**
     * LLM 생성 템플릿 개수 (활성화된 것만)
     *
     * @return 템플릿 개수
     */
    @Query("""
        SELECT COUNT(t) FROM MissionTemplate t
        WHERE t.llmGenerated = true
        AND t.active = true
        """)
    long countLLMGenerated();

    // ============= 관리자용 (권한 검증 없음) =============

    /**
     * 모든 템플릿 조회 (비활성화 포함)
     *
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    Page<MissionTemplate> findAll(Pageable pageable);

    /**
     * 비활성화된 템플릿만 조회
     *
     * @param pageable 페이징 정보
     * @return Page<MissionTemplate>
     */
    @Query("""
        SELECT t FROM MissionTemplate t
        WHERE t.active = false
        ORDER BY t.createdAt DESC
        """)
    Page<MissionTemplate> findInactiveTemplates(Pageable pageable);

}
