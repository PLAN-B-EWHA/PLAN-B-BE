package myexpressionfriend_api.child.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.child.domain.ChildPermissionType;

import java.util.Set;
import java.util.UUID;

/**
 * 아동 접근 권한 부여 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildAuthorizationCreateDTO {

    @NotNull(message = "권한을 부여할 사용자 ID는 필수입니다.")
    private UUID userId;

    /**
     * 부여할 권한 목록 (null 또는 비어있으면 빈 권한으로 생성)
     */
    private Set<ChildPermissionType> permissions;
}
