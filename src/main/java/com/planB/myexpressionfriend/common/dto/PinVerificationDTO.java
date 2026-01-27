package com.planB.myexpressionfriend.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PIN 검증 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinVerificationDTO {

    @NotBlank(message = "PIN은 필수입니다")
    @Pattern(regexp = "^\\d{4}$", message = "PIN은 4자리 숫자여야 합니다")
    private String pin;
}