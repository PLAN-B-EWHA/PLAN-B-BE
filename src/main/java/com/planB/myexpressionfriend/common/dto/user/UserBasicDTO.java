package com.planB.myexpressionfriend.common.dto.user;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
                .roles(user.getRoles())
                .build();
    }
}