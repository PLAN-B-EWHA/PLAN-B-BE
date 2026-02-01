package com.planB.myexpressionfriend.common.dto.child;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 주보호자 변경 요청 DTO (양육권 이전)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferPrimaryParentDTO {

    @NotNull(message = "새 주보호자 ID는 필수입니다")
    private UUID newPrimaryParentUserId;

    /**
     * Parental Gate PIN (주보호자만 변경 가능)
     */
    @NotNull(message = "PIN 검증은 필수입니다")
    private String pin;
}
