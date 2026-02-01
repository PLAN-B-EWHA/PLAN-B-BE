package com.planB.myexpressionfriend.common.dto.child;

import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * 아동 권한 부여 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildAuthorizationDTO {

    @NotNull(message = "사용자 ID는 필수입니다")
    private UUID userId;

    @NotEmpty(message = "권한은 최소 1개 이상 필요합니다")
    private Set<ChildPermissionType> permissions;

    /**
     * 주보호자 여부 (기본값: false)
     * 주의: PARENT 역할만 가능
     */
    private Boolean isPrimary;
}