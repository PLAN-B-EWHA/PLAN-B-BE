package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.domain.user.RoleChangeHistory;
import com.planB.myexpressionfriend.common.dto.user.UserResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserUpdateDTO;
import com.planB.myexpressionfriend.common.repository.RoleChangeHistoryRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import com.planB.myexpressionfriend.common.exception.EntityNotFoundException;
import com.planB.myexpressionfriend.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private static final Set<UserRole> PROMOTABLE_ROLES = Set.of(
            UserRole.PARENT,
            UserRole.TEACHER,
            UserRole.THERAPIST
    );

    private final UserRepository userRepository;
    private final RoleChangeHistoryRepository roleChangeHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        return UserResponseDTO.from(user);
    }

    public UserResponseDTO getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        return UserResponseDTO.from(user);
    }

    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return UserResponseDTO.fromList(users);
    }

    @Transactional
    public UserResponseDTO updateUser(String email, UserUpdateDTO updateDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
            user.changeName(updateDTO.getName());
        }

        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isEmpty()) {
            user.changePassword(passwordEncoder.encode(updateDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return UserResponseDTO.from(updatedUser);
    }

    @Transactional
    public UserResponseDTO promotePendingUser(UUID changedByUserId, UUID userId, UserRole targetRole) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (!PROMOTABLE_ROLES.contains(targetRole)) {
            throw new InvalidRequestException("PARENT, TEACHER, THERAPIST 역할만 승인 가능합니다.");
        }

        if (!user.hasRole(UserRole.PENDING)) {
            throw new InvalidRequestException("PENDING 상태의 사용자만 승인할 수 있습니다.");
        }

        String previousRoles = user.getRoles().stream()
                .map(UserRole::name)
                .sorted()
                .collect(Collectors.joining(","));

        user.changeToRole(targetRole);

        User savedUser = userRepository.save(user);

        roleChangeHistoryRepository.save(RoleChangeHistory.builder()
                .targetUserId(savedUser.getUserId())
                .changedByUserId(changedByUserId)
                .previousRoles(previousRoles)
                .newRole(targetRole)
                .build());

        log.info("User role promoted. userId={}, targetRole={}", userId, targetRole);
        return UserResponseDTO.from(savedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("사용자를 찾을 수 없습니다.");
        }
        userRepository.deleteById(userId);
    }
}
