package com.planB.myexpressionfriend.common.dto.user;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {

    private UUID userId;
    private String email;
    private String name;
    private Set<UserRole> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity → ResponseDTO 변환 (비밀번호 제외)
    public static UserResponseDTO from(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // UserDTO → ResponseDTO 변환
    public static UserResponseDTO from(UserDTO userDTO) {
        return UserResponseDTO.builder()
                .userId(userDTO.getUserId())
                .email(userDTO.getEmail())
                .name(userDTO.getName())
                .roles(userDTO.getRoles())
                .createdAt(userDTO.getCreatedAt())
                .updatedAt(userDTO.getUpdatedAt())
                .build();
    }

    // List 변환
    public static List<UserResponseDTO> fromList(List<User> users) {
        return users.stream()
                .map(UserResponseDTO::from)
                .collect(Collectors.toList());
    }
}
