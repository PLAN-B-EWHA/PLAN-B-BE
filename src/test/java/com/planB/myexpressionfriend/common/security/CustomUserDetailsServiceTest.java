package com.planB.myexpressionfriend.common.security;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

@SpringBootTest
@Slf4j
public class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    public void testLoadUserByUsername() {

        // Given 테스트 사용자 생성하기
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("테스트 사용자")
                .roles(Set.of(UserRole.PARENT, UserRole.THERAPIST))
                .build();
        userRepository.save(user);

        // When 사용자 조회하기
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Then 검증하기
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getAuthorities()).hasSize(2);

        log.info("조회된 사용자:  {}", userDetails.getAuthorities());
        log.info("권한: {}", userDetails.getAuthorities());

    }

    @Test
    void testLoadUserByUsername_NotFound() {
        // When Then
        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername("notexist@example.com")
        ).isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }


}
