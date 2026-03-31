package myexpressionfriend_api.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.auth.dto.UserBasicDTO;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.domain.ChildrenAuthorizedUser;

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
public class AuthorizedUserResponseDTO {

    private UUID authorizationId;
    private UserBasicDTO user;
    private Set<ChildPermissionType> permissions;
    private Boolean isPrimary;
    private UserBasicDTO authorizedBy;
    private LocalDateTime authorizedAt;

    /**
     * Entity → DTO 변환
     */
    public static AuthorizedUserResponseDTO from(ChildrenAuthorizedUser authorizedUser) {
        boolean isPrimary = Boolean.TRUE.equals(authorizedUser.getIsPrimary());

        return AuthorizedUserResponseDTO.builder()
                .authorizationId(authorizedUser.getAuthorizationId())
                .user(UserBasicDTO.from(authorizedUser.getUser()))
                .permissions(isPrimary
                        ? Set.of(ChildPermissionType.values())
                        : copySet(authorizedUser.getPermissions()))
                .isPrimary(isPrimary)
                .authorizedBy(authorizedUser.getAuthorizedBy() != null
                        ? UserBasicDTO.from(authorizedUser.getAuthorizedBy())
                        : null)
                .authorizedAt(authorizedUser.getAuthorizedAt())
                .build();
    }

    private static <T> Set<T> copySet(Set<T> source) {
        return source == null ? Set.of() : Set.copyOf(source);
    }
}
