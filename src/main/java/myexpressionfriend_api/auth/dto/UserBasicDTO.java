package myexpressionfriend_api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.domain.user.UserRole;

import java.util.Set;
import java.util.UUID;

/**
 * 사용자 기본 정보 DTO (민감 정보 제외)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicDTO {

    private UUID userId;
    private String email;
    private String name;
    private Set<UserRole> roles;

    /**
     * Entity → DTO 변환
     */
    public static UserBasicDTO from(User user) {
        return UserBasicDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(copySet(user.getRoles()))
                .build();
    }

    private static <T> Set<T> copySet(Set<T> source) {
        return source == null ? Set.of() : Set.copyOf(source);
    }
}
