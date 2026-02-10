package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import com.planB.myexpressionfriend.common.dto.mission.*;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.repository.MissionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * MissionTemplate Service
 *
 * 책임:
 * - 미션 템플릿 CRUD
 * - 카테고리/난이도 별 조회
 * - LLM 생성 템플릿 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MissionTemplateService {

    private final MissionTemplateRepository templateRepository;

    // ============= 템플릿 생성 =============

    /**
     * 템플릿 생성 (치료사만 가능)
     *
     * @param dto 생성 요청 DTO
     * @return 생성된 템플릿 DTO
     */
    @Transactional
    public MissionTemplateDTO createTemplate(MissionTemplateCreateDTO dto) {
        log.info("템플릿 생성 시작 - category: {}, difficulty: {}",
                dto.getCategory(), dto.getDifficulty());

        MissionTemplate template = MissionTemplate.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .difficulty(dto.getDifficulty())
                .instructions(dto.getInstructions())
                .expectedDuration(dto.getExpectedDuration())
                .llmGenerated(dto.getLlmGenerated() != null ? dto.getLlmGenerated() : false)
                .active(true)
                .isDeleted(false)
                .build();

        MissionTemplate saved = templateRepository.save(template);
        log.info("템플릿 생성 완료 - templateId: {}", saved.getTemplateId());

        return MissionTemplateDTO.from(saved);
    }

    // ============= 템플릿 조회 =============

    /**
     * 템플릿 상세 조회
     *
     * @param templateId 템플릿 ID
     * @return 템플릿 DTO
     */
    public MissionTemplateDTO getTemplate(UUID templateId) {
        log.debug("템플릿 조회 - templateId: {}", templateId);

        MissionTemplate template = templateRepository.findByIdAndActive(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 비활성화된 템플릿입니다"));

        return MissionTemplateDTO.from(template);
    }

    /**
     * 모든 활성화된 템플릿 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 템플릿 목록 (페이징)
     */
    public PageResponseDTO<MissionTemplateDTO> getAllTemplates(Pageable pageable) {
        log.debug("모든 템플릿 조회 - page: {}", pageable.getPageNumber());

        Page<MissionTemplate> templatePage = templateRepository.findAllActive(pageable);
        return PageResponseDTO.from(templatePage, MissionTemplateDTO::from);
    }

    /**
     * 카테고리별 템플릿 조회 (페이징)
     *
     * @param category 카테고리
     * @param pageable 페이징 정보
     * @return 템플릿 목록 (페이징)
     */
    public PageResponseDTO<MissionTemplateDTO> getTemplatesByCategory(
            MissionCategory category,
            Pageable pageable
    ) {
        log.debug("카테고리별 템플릿 조회 - category: {}, page: {}",
                category, pageable.getPageNumber());

        Page<MissionTemplate> templatePage =
                templateRepository.findByCategoryAndActive(category, pageable);

        return PageResponseDTO.from(templatePage, MissionTemplateDTO::from);
    }

    /**
     * 난이도별 템플릿 조회 (페이징)
     *
     * @param difficulty 난이도
     * @param pageable 페이징 정보
     * @return 템플릿 목록 (페이징)
     */
    public PageResponseDTO<MissionTemplateDTO> getTemplatesByDifficulty(
            MissionDifficulty difficulty,
            Pageable pageable
    ) {
        log.debug("난이도별 템플릿 조회 - difficulty: {}, page: {}",
                difficulty, pageable.getPageNumber());

        Page<MissionTemplate> templatePage =
                templateRepository.findByDifficultyAndActive(difficulty, pageable);

        return PageResponseDTO.from(templatePage, MissionTemplateDTO::from);
    }

    /**
     * 템플릿 검색/필터링
     *
     * @param searchDTO 검색 조건
     * @return 검색 결과 (페이징)
     */
    public PageResponseDTO<MissionTemplateDTO> searchTemplates(MissionTemplateSearchDTO searchDTO) {
        log.debug("템플릿 검색 - category: {}, difficulty: {}, keyword: {}",
                searchDTO.getCategory(), searchDTO.getDifficulty(), searchDTO.getKeyword());

        searchDTO.validate();
        Pageable pageable = searchDTO.toPageable();

        Page<MissionTemplate> templatePage;

        // 검색 조건에 따라 다른 Repository 메서드 호출
        if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isBlank()) {
            // 키워드 검색
            templatePage = templateRepository.searchByKeywordAndActive(
                    searchDTO.getKeyword(),
                    pageable
            );
        } else if (searchDTO.getCategory() != null && searchDTO.getDifficulty() != null) {
            // 카테고리 + 난이도
            templatePage = templateRepository.findByCategoryAndDifficultyAndActive(
                    searchDTO.getCategory(),
                    searchDTO.getDifficulty(),
                    pageable
            );
        } else if (searchDTO.getCategory() != null) {
            // 카테고리만
            templatePage = templateRepository.findByCategoryAndActive(
                    searchDTO.getCategory(),
                    pageable
            );
        } else if (searchDTO.getDifficulty() != null) {
            // 난이도만
            templatePage = templateRepository.findByDifficultyAndActive(
                    searchDTO.getDifficulty(),
                    pageable
            );
        } else if (Boolean.TRUE.equals(searchDTO.getLlmGenerated())) {
            // LLM 생성 템플릿만
            templatePage = templateRepository.findLLMGeneratedTemplates(pageable);
        } else if (Boolean.FALSE.equals(searchDTO.getLlmGenerated())) {
            // 수동 생성 템플릿만
            templatePage = templateRepository.findManualTemplates(pageable);
        } else {
            // 조건 없음 - 전체 조회
            templatePage = templateRepository.findAllActive(pageable);
        }

        return PageResponseDTO.from(templatePage, MissionTemplateDTO::from);
    }

    // ============= 템플릿 수정 =============

    /**
     * 템플릿 수정 (치료사만 가능)
     *
     * @param templateId 템플릿 ID
     * @param dto 수정 요청 DTO
     * @return 수정된 템플릿 DTO
     */
    @Transactional
    public MissionTemplateDTO updateTemplate(UUID templateId, MissionTemplateUpdateDTO dto) {
        log.info("템플릿 수정 시작 - templateId: {}", templateId);

        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다"));

        // 필드별 수정
        if (dto.getTitle() != null) {
            template.changeTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            template.changeDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            template.changeCategory(dto.getCategory());
        }
        if (dto.getDifficulty() != null) {
            template.changeDifficulty(dto.getDifficulty());
        }
        if (dto.getInstructions() != null) {
            template.changeInstructions(dto.getInstructions());
        }
        if (dto.getExpectedDuration() != null) {
            template.changeExpectedDuration(dto.getExpectedDuration());
        }

        log.info("템플릿 수정 완료 - templateId: {}", templateId);
        return MissionTemplateDTO.from(template);
    }

    // ============= 템플릿 활성화/비활성화 =============

    /**
     * 템플릿 활성화
     *
     * @param templateId 템플릿 ID
     */
    @Transactional
    public void activateTemplate(UUID templateId) {
        log.info("템플릿 활성화 - templateId: {}", templateId);

        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다"));

        template.activate();
    }

    /**
     * 템플릿 비활성화
     *
     * @param templateId 템플릿 ID
     */
    @Transactional
    public void deactivateTemplate(UUID templateId) {
        log.info("템플릿 비활성화 - templateId: {}", templateId);

        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다"));

        template.deactivate();
    }

    // ============= 템플릿 삭제 =============

    /**
     * 템플릿 삭제 (Soft Delete)
     *
     * @param templateId 템플릿 ID
     */
    @Transactional
    public void deleteTemplate(UUID templateId) {
        log.info("템플릿 삭제 시작 - templateId: {}", templateId);

        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다"));

        template.delete();
        log.info("템플릿 삭제 완료 - templateId: {}", templateId);
    }

    // ============= 통계 =============

    /**
     * 활성화된 템플릿 총 개수
     *
     * @return 템플릿 개수
     */
    public long countActiveTemplates() {
        return templateRepository.countActive();
    }

    /**
     * 카테고리별 템플릿 개수
     *
     * @param category 카테고리
     * @return 템플릿 개수
     */
    public long countTemplatesByCategory(MissionCategory category) {
        return templateRepository.countByCategory(category);
    }

    /**
     * LLM 생성 템플릿 개수
     *
     * @return 템플릿 개수
     */
    public long countLLMGeneratedTemplates() {
        return templateRepository.countLLMGenerated();
    }


}
