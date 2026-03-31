package myexpressionfriend_api.child.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PIN 설정/변경 요청 DTO
 * - 최초 설정: currentPin null
 * - 변경: currentPin 필수
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinUpdateDTO {

    @Pattern(regexp = "^\\d{4}$", message = "현재 PIN은 4자리 숫자여야 합니다.")
    private String currentPin;

    @NotBlank(message = "새 PIN은 필수입니다.")
    @Pattern(regexp = "^\\d{4}$", message = "새 PIN은 4자리 숫자여야 합니다.")
    private String newPin;
}
