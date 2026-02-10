package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 미션 템플릿 응답 DTO
 */
@Getter
@Builder
public class MissionTemplateDTO {

    private UUID templateId;
    private String title;
    private String description;
    private MissionCategory category;
    private MissionDifficulty difficulty;
    private String instructions;
    private Integer expectedDuration;
    private Boolean llmGenerated;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static MissionTemplateDTO from(MissionTemplate template) {
        if (template == null) {
            return null;
        }

        return MissionTemplateDTO.builder()
                .templateId(template.getTemplateId())
                .title(template.getTitle())
                .description(template.getDescription())
                .category(template.getCategory())
                .difficulty(template.getDifficulty())
                .instructions(template.getInstructions())
                .expectedDuration(template.getExpectedDuration())
                .llmGenerated(template.getLlmGenerated())
                .active(template.getActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}