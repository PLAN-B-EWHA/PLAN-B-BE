package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

@SpringBootTest
@Slf4j
@Transactional
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testInsert() {

        // Given
        User user = User.builder()
                .email("test1@user.com")
                .password(passwordEncoder.encode("testPassword"))
                .name("test1")
                .roles(Set.of(UserRole.PARENT))
                .build();

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test1@user.com");
        assertThat(savedUser.getRoles()).contains(UserRole.PARENT);

        log.info("저장된 사용자: {}", savedUser);
    }

    @Test
    public void testFindByEmail() {

        //when
        Optional<User> found =  userRepository.findByEmail("test1@user.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test1");
    }

    @Test
    public void testFindByEmailWithRoles(){

        // When
        Optional<User> found = userRepository.findByEmailWithRoles("test1@user.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).hasSize(1);
        assertThat(found.get().getRoles().contains(UserRole.PARENT));

        log.info("조회된 사용자 역할: {}", found.get().getRoles());
    }

    @Test
    public void testExistsByEmail() {

        // When & Then
        assertThat(userRepository.existsByEmail("test1@user.com")).isTrue();
        assertThat(userRepository.existsByEmail("fdsf@gmail.com")).isFalse();
    }

    @Test
    public void testCreateTestDate() {
        // 테스트용 사용자 10명 생성
        for (int i = 1; i <= 14; i++) {
            UserRole role;
            if (i <= 5) {
                role = UserRole.PARENT;
            } else if (i <= 8) {
                role = UserRole.THERAPIST;
            } else if (i <= 12) {
                role = UserRole.TEACHER;
            } else {
                role = UserRole.ADMIN;
            }

            User user = User.builder()
                    .email("user" + i + "@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .name("사용자" + i)
                    .roles(Set.of(role))
                    .build();

            userRepository.save(user);
        }

        assertThat(userRepository.count()).isGreaterThan(10L);
        log.info("총 사용자 수: {}", userRepository.count());
    }
}
