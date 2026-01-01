package com.jun_bank.auth_server.global.config;

import com.jun_bank.auth_server.global.security.HeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring Security 설정 (Auth Server)
 * <p>
 * JWT 발급 서버로서의 보안 설정을 정의합니다.
 * 로그인/회원가입 등의 인증 관련 엔드포인트는 인증 없이 접근 가능합니다.
 *
 * <h3>특징:</h3>
 * <ul>
 *   <li>JWT 발급 엔드포인트: 인증 불필요</li>
 *   <li>JWKS 엔드포인트: 인증 불필요 (Public Key 제공)</li>
 *   <li>내부 서비스 호출: Gateway에서 전달받은 헤더로 인증</li>
 *   <li>Security 예외는 GlobalExceptionHandler로 위임</li>
 * </ul>
 *
 * <h3>인증 제외 경로:</h3>
 * <ul>
 *   <li>/api/v1/auth/login - 로그인</li>
 *   <li>/api/v1/auth/register - 회원가입</li>
 *   <li>/api/v1/auth/refresh - 토큰 갱신</li>
 *   <li>/api/v1/auth/validate - 토큰 검증 (Gateway용)</li>
 *   <li>/api/v1/auth/public-key - Public Key 조회 (PEM)</li>
 *   <li>/api/v1/auth/public-key/info - Public Key 정보</li>
 *   <li>/.well-known/jwks.json - JWKS 표준 엔드포인트</li>
 *   <li>/internal/** - 내부 서비스 통신</li>
 *   <li>/actuator/** - 헬스체크</li>
 *   <li>/swagger-ui/**, /v3/api-docs/** - API 문서</li>
 * </ul>
 *
 * @see HeaderAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final HeaderAuthenticationFilter headerAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
  ) throws Exception {
    return http
        // CSRF 비활성화 (Stateless REST API)
        .csrf(AbstractHttpConfigurer::disable)

        // 세션 미사용 (Stateless)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 요청 권한 설정
        .authorizeHttpRequests(auth -> auth
            // ========================================
            // 인증 불필요 (Public)
            // ========================================
            // Actuator 엔드포인트
            .requestMatchers("/actuator/**").permitAll()
            // Swagger UI
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

            // ========================================
            // JWKS 표준 엔드포인트 (Public Key 제공)
            // ========================================
            .requestMatchers("/.well-known/jwks.json").permitAll()

            // ========================================
            // 인증 API (로그인/회원가입 등)
            // ========================================
            .requestMatchers("/api/v1/auth/login").permitAll()
            .requestMatchers("/api/v1/auth/register").permitAll()
            .requestMatchers("/api/v1/auth/refresh").permitAll()
            .requestMatchers("/api/v1/auth/validate").permitAll()
            .requestMatchers("/api/v1/auth/public-key").permitAll()
            .requestMatchers("/api/v1/auth/public-key/info").permitAll()

            // ========================================
            // 내부 API (서비스 간 통신)
            // ========================================
            .requestMatchers("/internal/**").permitAll()

            // ========================================
            // 나머지 요청은 인증 필요
            // ========================================
            .anyRequest().authenticated()
        )

        // Security Filter 레벨 예외를 GlobalExceptionHandler로 위임
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint((request, response, authException) ->
                exceptionResolver.resolveException(request, response, null, authException))
            .accessDeniedHandler((request, response, accessDeniedException) ->
                exceptionResolver.resolveException(request, response, null, accessDeniedException))
        )

        // 헤더 기반 인증 필터 추가
        .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        .build();
  }

  /**
   * 비밀번호 암호화 인코더
   * <p>
   * BCrypt 알고리즘을 사용하여 비밀번호를 암호화합니다.
   * 회원가입 시 비밀번호 암호화, 로그인 시 비밀번호 검증에 사용됩니다.
   * </p>
   *
   * @return PasswordEncoder 빈
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}