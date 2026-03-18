package com.planB.myexpressionfriend.unity.dto.llm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * LLM Unity 미션 배치 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmMissionBatchDTO {

    @Valid
    @NotEmpty
    private List<UnityLlmMissionDTO> missions;
}
