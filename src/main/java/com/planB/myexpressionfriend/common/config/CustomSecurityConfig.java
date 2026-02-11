package com.planB.myexpressionfriend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.repository.GameSessionRepository;
import com.planB.myexpressionfriend.common.security.filter.GameSessionAuthenticationFilter;
import com.planB.myexpressionfriend.common.security.filter.JWTCheckFilter;
import com.planB.myexpressionfriend.common.security.handler.APILoginFailHandler;
import com.planB.myexpressionfriend.common.security.handler.APILoginSuccessHandler;
import com.planB.myexpressionfriend.common.security.handler.CustomAccessDeniedHandler;
import com.planB.myexpressionfriend.common.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableMethodSecurity
public class CustomSecurityConfig {

    private final APILoginSuccessHandler loginSuccessHandler;
    private final APILoginFailHandler loginFailHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final ApplicationContext context;

    @Bean
    public JWTCheckFilter jwtCheckFilter(
            JWTUtil jwtUtil,
            ObjectMapper objectMapper
    ) {
        return new JWTCheckFilter(jwtUtil, objectMapper);
    }

    @Bean
    public GameSessionAuthenticationFilter gameSessionAuthenticationFilter(
            GameSessionRepository sessionRepository
    ) {
        return new GameSessionAuthenticationFilter(sessionRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("===============Security Filter Chain Config==============");

        // CORS 설정
        http.cors(httpSecurityCorsConfigurer -> {
            httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());
        });

        // Session을 Stateless로 설정 (JWT 사용)
        http.sessionManagement(sessionConfig ->
                sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // CSRF 비활성화 (REST API 서버)
        http.csrf(csrf -> csrf.disable());

        // 엔드포인트별 접근 제어
        http.authorizeHttpRequests(auth -> auth
                // 게임 세션 검증 인증 불필요
                .requestMatchers("/api/game-sessions/validate").permitAll()
                .requestMatchers("/api/game-sessions/refresh").permitAll()

                // 게임 API는 게임 세션 필터에서 처리
                .requestMatchers("/api/game/**").permitAll()
                .requestMatchers("/api/unity/**").permitAll()

                // Swagger 경로 허용
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api-docs/**"
                ).permitAll()

                // 인증 없이 접근 가능한 엔드포인트
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()

                // 나머지는 인증 필요
                .anyRequest().authenticated()
        );

        // ✅ 필터 등록 (순서: GameSession → JWT → UsernamePassword)
        http.addFilterBefore(
                context.getBean(GameSessionAuthenticationFilter.class),
                UsernamePasswordAuthenticationFilter.class
        );

        http.addFilterBefore(
                context.getBean(JWTCheckFilter.class),
                UsernamePasswordAuthenticationFilter.class
        );

        // Form Login 설정
        http.formLogin(form -> form
                .loginPage("/api/auth/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailHandler)
        );

        // 접근 거부 Handler 등록
        http.exceptionHandling(exception -> exception
                .accessDeniedHandler(accessDeniedHandler)
        );

        log.info("============= Security Filter Chain 설정 완료 =============");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        log.info("============= CORS 설정 시작 =============");

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("============= CORS 설정 완료 =============");
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}