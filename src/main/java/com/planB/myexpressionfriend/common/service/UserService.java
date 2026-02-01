package com.planB.myexpressionfriend.common.service;


import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.user.UserResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserUpdateDTO;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일로 사용자 조회
     */
    public UserResponseDTO getUserByEmail(String email) {
        log.info("사용자 조회: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return UserResponseDTO.from(user);
    }

    /**
     * ID로 사용자 조회
     */
    public UserResponseDTO getUserById(UUID userId) {
        log.info("사용자 조회: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return UserResponseDTO.from(user);
    }

    /**
     * 전체 사용자 조회
     */
    public List<UserResponseDTO> getAllUsers() {
        log.info("전체 사용자 조회");

        List<User> users = userRepository.findAll();

        return UserResponseDTO.fromList(users);
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserResponseDTO updateUser(String email, UserUpdateDTO updateDTO) {
        log.info("사용자 정보 수정: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 이름 변경
        if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
            user.changeName(updateDTO.getName());
            log.info("이름 변경: {}", updateDTO.getName());
        }

        // 비밀번호 변경
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isEmpty()) {
            user.changePassword(passwordEncoder.encode(updateDTO.getPassword()));
            log.info("비밀번호 변경 완료");
        }

        User updatedUser = userRepository.save(user);

        return UserResponseDTO.from(updatedUser);
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public void deleteUser(UUID userId) {
        log.info("사용자 삭제: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("사용자를 찾을 수 없습니다");
        }

        userRepository.deleteById(userId);

        log.info("사용자 삭제 완료");
    }
}
