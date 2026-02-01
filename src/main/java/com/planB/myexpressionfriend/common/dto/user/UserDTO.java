package com.planB.myexpressionfriend.common.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "password")
public class UserDTO implements UserDetails {

    private UUID userId;
    private String email;

    @JsonIgnore
    private String password;

    private String name;
    private Set<UserRole> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ============= Spring Security UserDetails 구현 =============


    @Override
    public boolean isAccountNonExpired() {
        return true;    // 계정 만료 기능 미사용
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;    // 계정 잠금 기능 미사용
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;    // 비밀번호 만료 기능 미사용
    }

    @Override
    public boolean isEnabled() {
        return true;    // 계정 활성화 기능 미사용
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return this.email;  // 이메일을 username으로 사용
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    // ============= JWT Claims용 메서드 =============

    /**
     * JWT 토큰에 저장할 Claims 생성
     */
    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("email", email);
        claims.put("name", name);
        claims.put("roles", roles.stream()
                .map(UserRole::name)
                .collect(Collectors.toList()));
        return claims;
    }

    // ============= Entity ↔ DTO 변환 =============

    /**
     * User Entity → UserDTO 변환
     */
    public static UserDTO from(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .password(user.getPassword())
                .name(user.getName())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * User Entity List → UserDTO List 변환
     */
    public static List<UserDTO> fromList(List<User> users) {
        return users.stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }


}
