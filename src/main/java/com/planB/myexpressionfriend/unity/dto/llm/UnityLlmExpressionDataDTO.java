package com.planB.myexpressionfriend.unity.dto.llm;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * LLM 표정 미션 상세 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityLlmExpressionDataDTO {

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> characterDialogue;

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> successFeedback;

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> retryFeedback;

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> failFeedback;
}
