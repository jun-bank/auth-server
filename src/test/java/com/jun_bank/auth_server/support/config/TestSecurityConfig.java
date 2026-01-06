package com.jun_bank.auth_server.support.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트용 Security 설정
 * <p>
 * 메인 SecurityConfig를 오버라이드합니다.
 * Bean 이름을 동일하게(securityFilterChain) 하여 완전히 대체합니다.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * 메인 SecurityConfig의 securityFilterChain을 오버라이드
     * Bean 이름이 같아야 오버라이드됨
     */
    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}