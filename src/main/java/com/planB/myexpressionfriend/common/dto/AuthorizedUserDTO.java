package com.planB.myexpressionfriend.common.dto;

import com.planB.myexpressionfriend.common.domain.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.ChildrenAuthorizedUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 권한 부여된 사용자 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizedUserDTO {

    private UUID authorizationId;
    private UserBasicDTO user;
    private Set<ChildPermissionType> permissions;
    private Boolean isPrimary;
    private UserBasicDTO authorizedBy;
    private LocalDateTime authorizedAt;

    /**
     * Entity → DTO 변환
     */
    public static AuthorizedUserDTO from(ChildrenAuthorizedUser authorizedUser) {
        return AuthorizedUserDTO.builder()
                .authorizationId(authorizedUser.getAuthorizationId())
                .user(UserBasicDTO.from(authorizedUser.getUser()))
                .permissions(authorizedUser.getPermissions())
                .isPrimary(authorizedUser.getIsPrimary())
                .authorizedBy(authorizedUser.getAuthorizedBy() != null
                        ? UserBasicDTO.from(authorizedUser.getAuthorizedBy())
                        : null)
                .authorizedAt(authorizedUser.getAuthorizedAt())
                .build();
    }
}