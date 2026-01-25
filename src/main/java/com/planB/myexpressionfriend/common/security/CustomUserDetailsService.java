package com.planB.myexpressionfriend.common.security;

import com.planB.myexpressionfriend.common.domain.User;
import com.planB.myexpressionfriend.common.dto.UserDTO;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("------------loadUserByUsername------------");
        log.info("username(email): {}", username);

        // 이메일로 사용자 조회하기
        User user = userRepository.findByEmailWithRoles(username)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음: {}", username );
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        log.info("사용자 조회 성공: {}", user.getEmail());
        log.info("사용자 역할: {}", user.getRoles());

        // User Entity -> UserDTO 변환 (UserDTO가 UserDetails 구현)
        UserDTO userDTO = UserDTO.from(user);

        log.info("UserDTO 변환 완료, 권한: {}", userDTO.getAuthorities());

        return userDTO;
    }
}
