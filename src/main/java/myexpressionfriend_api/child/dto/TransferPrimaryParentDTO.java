package myexpressionfriend_api.child.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 주보호자 권한 이전 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferPrimaryParentDTO {

    @NotNull(message = "새 주보호자 사용자 ID는 필수입니다.")
    private UUID newPrimaryUserId;
}
