package com.planB.myexpressionfriend.unity.dto.llm;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * LLM 상황 선택지 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmSituationOptionDTO {

    @NotNull
    @Positive
    private Integer id;

    @NotNull
    @Size(min = 1, max = 30)
    private String text;

    @NotNull
    private Boolean isCorrect;

    @NotEmpty
    private List<@Size(min = 1, max = 30) String> feedback;
}
