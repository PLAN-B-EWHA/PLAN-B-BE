package com.planB.myexpressionfriend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.repository.GameSessionRepository;
import com.planB.myexpressionfriend.common.security.filter.GameSessionAuthenticationFilter;
import com.planB.myexpressionfriend.common.security.filter.JWTCheckFilter;
import com.planB.myexpressionfriend.common.security.handler.APILoginFailHandler;
import com.planB.myexpressionfriend.common.security.handler.APILoginSuccessHandler;
import com.planB.myexpressionfriend.common.security.handler.CustomAccessDeniedHandler;
import com.planB.myexpressionfriend.common.util.JWTUtil;
import jakarta.servlet.DispatcherType;
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

        // CORS ?ㅼ젙
        http.cors(httpSecurityCorsConfigurer -> {
            httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());
        });

        http.sessionManagement(sessionConfig ->
                sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.csrf(csrf -> csrf.disable());

        // ?붾뱶?ъ씤?몃퀎 ?묎렐 ?쒖뼱
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/game/**").permitAll()
                .requestMatchers("/api/unity/**").permitAll()

                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api-docs/**"
                ).permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/error").permitAll()

                .anyRequest().authenticated()
        );

        http.addFilterBefore(
                context.getBean(GameSessionAuthenticationFilter.class),
                UsernamePasswordAuthenticationFilter.class
        );

        http.addFilterBefore(
                context.getBean(JWTCheckFilter.class),
                UsernamePasswordAuthenticationFilter.class
        );



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
        configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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
