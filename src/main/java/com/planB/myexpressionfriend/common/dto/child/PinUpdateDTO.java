package com.planB.myexpressionfriend.common.dto.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PIN 설정/변경 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinUpdateDTO {

    /**
     * 현재 PIN (변경 시 필요)
     */
    @Pattern(regexp = "^\\d{4}$", message = "현재 PIN은 4자리 숫자여야 합니다")
    private String currentPin;

    /**
     * 새로운 PIN
     */
    @NotBlank(message = "새 PIN은 필수입니다")
    @Pattern(regexp = "^\\d{4}$", message = "새 PIN은 4자리 숫자여야 합니다")
    private String newPin;
}