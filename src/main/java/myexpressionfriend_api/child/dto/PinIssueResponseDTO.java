package myexpressionfriend_api.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 임시 PIN 발급 응답 DTO
 * - 단 한 번만 확인 가능한 임시 PIN 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinIssueResponseDTO {

    private String pin;
}
