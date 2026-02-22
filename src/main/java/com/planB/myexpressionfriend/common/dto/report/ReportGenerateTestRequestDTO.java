package com.planB.myexpressionfriend.common.dto.report;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerateTestRequestDTO {

    private UUID targetChildId;
    private String promptOverride;

    @Positive(message = "maxTokens must be positive")
    private Integer maxTokens;
}
