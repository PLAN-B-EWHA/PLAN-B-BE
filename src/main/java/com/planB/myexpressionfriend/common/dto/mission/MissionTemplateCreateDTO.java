package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * 미션 템플릿 생성 요청 DTO
 */
@Getter
@Builder
public class MissionTemplateCreateDTO {

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    @Size(max = 50000, message = "설명은 50,000자를 초과할 수 없습니다")
    private String description;

    @NotNull(message = "카테고리는 필수입니다")
    private MissionCategory category;

    @NotNull(message = "난이도는 필수입니다")
    private MissionDifficulty difficulty;

    @NotBlank(message = "수행 방법은 필수입니다")
    @Size(max = 50000, message = "수행 방법은 50,000자를 초과할 수 없습니다")
    private String instructions;

    @Positive(message = "예상 소요 시간은 양수여야 합니다")
    private Integer expectedDuration;

    private Boolean llmGenerated;
}