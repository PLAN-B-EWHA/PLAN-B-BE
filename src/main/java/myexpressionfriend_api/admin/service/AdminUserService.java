package myexpressionfriend_api.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.domain.user.UserRole;
import myexpressionfriend_api.auth.dto.UserResponseDTO;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAllWithRoles();
        return UserResponseDTO.fromList(users);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(UUID targetUserId) {
        User user = userRepository.findByIdWithRoles(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
        return UserResponseDTO.from(user);
    }

    public UserResponseDTO changeUserRole(UUID adminUserId, UUID targetUserId, UserRole newRole) {
        if (adminUserId.equals(targetUserId)) {
            throw new InvalidRequestException("자기 자신의 역할은 변경할 수 없습니다.");
        }

        User targetUser = userRepository.findByIdWithRoles(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

        UserRole previousRole = targetUser.getRoles().iterator().next();
        targetUser.changeToRole(newRole);

        log.info("Admin {} changed user {} role: {} -> {}", adminUserId, targetUserId, previousRole, newRole);

        return UserResponseDTO.from(targetUser);
    }
}
