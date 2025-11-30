package com.jun_bank.auth_server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 (Auth Server)
 * - JWT 발급 서버로서 로그인/회원가입 엔드포인트는 인증 없이 접근 가능
 * - 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 비활성화 (Stateless REST API)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 미사용 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Actuator 엔드포인트 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 인증 관련 엔드포인트 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        // 내부 API (서비스 간 통신) 허용
                        .requestMatchers("/internal/**").permitAll()
                        // 나머지 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}