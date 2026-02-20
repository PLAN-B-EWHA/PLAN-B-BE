package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateCreateDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateSearchDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateUpdateDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.repository.MissionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MissionTemplateService {

    private final MissionTemplateRepository templateRepository;

    @Transactional
    public MissionTemplateDTO createTemplate(MissionTemplateCreateDTO dto) {
        MissionTemplate template = MissionTemplate.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .difficulty(dto.getDifficulty())
                .instructions(dto.getInstructions())
                .expectedDuration(dto.getExpectedDuration())
                .llmGenerated(dto.getLlmGenerated() != null && dto.getLlmGenerated())
                .active(true)
                .isDeleted(false)
                .build();

        MissionTemplate saved = templateRepository.save(template);
        return MissionTemplateDTO.from(saved);
    }

    public MissionTemplateDTO getTemplate(UUID templateId) {
        MissionTemplate template = templateRepository.findByIdAndActive(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 비활성화된 템플릿입니다."));
        return MissionTemplateDTO.from(template);
    }

    public PageResponseDTO<MissionTemplateDTO> getAllTemplates(Pageable pageable) {
        Page<MissionTemplate> page = templateRepository.findAllActive(pageable);
        return PageResponseDTO.from(page, MissionTemplateDTO::from);
    }

    public PageResponseDTO<MissionTemplateDTO> getTemplatesByCategory(MissionCategory category, Pageable pageable) {
        Page<MissionTemplate> page = templateRepository.findByCategoryAndActive(category, pageable);
        return PageResponseDTO.from(page, MissionTemplateDTO::from);
    }

    public PageResponseDTO<MissionTemplateDTO> getTemplatesByDifficulty(MissionDifficulty difficulty, Pageable pageable) {
        Page<MissionTemplate> page = templateRepository.findByDifficultyAndActive(difficulty, pageable);
        return PageResponseDTO.from(page, MissionTemplateDTO::from);
    }

    public PageResponseDTO<MissionTemplateDTO> searchTemplates(MissionTemplateSearchDTO searchDTO) {
        searchDTO.validate();
        Pageable pageable = searchDTO.toPageable();

        Page<MissionTemplate> page;
        if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isBlank()) {
            page = templateRepository.searchByKeywordAndActive(searchDTO.getKeyword(), pageable);
        } else if (searchDTO.getCategory() != null && searchDTO.getDifficulty() != null) {
            page = templateRepository.findByCategoryAndDifficultyAndActive(
                    searchDTO.getCategory(), searchDTO.getDifficulty(), pageable
            );
        } else if (searchDTO.getCategory() != null) {
            page = templateRepository.findByCategoryAndActive(searchDTO.getCategory(), pageable);
        } else if (searchDTO.getDifficulty() != null) {
            page = templateRepository.findByDifficultyAndActive(searchDTO.getDifficulty(), pageable);
        } else if (Boolean.TRUE.equals(searchDTO.getLlmGenerated())) {
            page = templateRepository.findLLMGeneratedTemplates(pageable);
        } else if (Boolean.FALSE.equals(searchDTO.getLlmGenerated())) {
            page = templateRepository.findManualTemplates(pageable);
        } else {
            page = templateRepository.findAllActive(pageable);
        }

        return PageResponseDTO.from(page, MissionTemplateDTO::from);
    }

    @Transactional
    public MissionTemplateDTO updateTemplate(UUID templateId, MissionTemplateUpdateDTO dto) {
        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다."));

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

        return MissionTemplateDTO.from(template);
    }

    @Transactional
    public void activateTemplate(UUID templateId) {
        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다."));
        template.activate();
    }

    @Transactional
    public void deactivateTemplate(UUID templateId) {
        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다."));
        template.deactivate();
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        MissionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿입니다."));
        template.delete();
    }

    public long countActiveTemplates() {
        return templateRepository.countActive();
    }

    public long countTemplatesByCategory(MissionCategory category) {
        return templateRepository.countByCategory(category);
    }

    public long countLLMGeneratedTemplates() {
        return templateRepository.countLLMGenerated();
    }
}
