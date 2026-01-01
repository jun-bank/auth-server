package com.jun_bank.auth_server.global.security;

import com.jun_bank.auth_server.domain.auth.domain.model.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Gateway에서 전달받은 헤더 기반 인증 필터
 * <p>
 * API Gateway에서 JWT 검증 후 전달하는 사용자 정보 헤더를 읽어
 * SecurityContext에 Authentication을 설정합니다.
 *
 * <h3>처리하는 헤더:</h3>
 * <ul>
 *   <li>X-User-Id: 사용자 ID (USR-xxx)</li>
 *   <li>X-User-Role: 사용자 역할 (USER/ADMIN)</li>
 *   <li>X-User-Email: 사용자 이메일</li>
 *   <li>X-User-Type: 사용자 유형 (CUSTOMER/SELLER)</li>
 * </ul>
 *
 * <h3>인증 흐름:</h3>
 * <pre>
 * Client → Gateway (JWT 검증) → Auth Server (헤더 기반 인증)
 *                               ↓
 *                        X-User-Id 헤더 확인
 *                               ↓
 *                        UserPrincipal 생성
 *                               ↓
 *                        SecurityContext 설정
 * </pre>
 *
 * <h3>주의사항:</h3>
 * <p>
 * Auth Server는 JWT 발급 서버이므로, 로그인/회원가입 등의 엔드포인트는
 * 이 필터를 통과하더라도 인증 없이 접근 가능해야 합니다.
 * SecurityConfig에서 해당 경로를 permitAll로 설정합니다.
 * </p>
 *
 * @see UserPrincipal
 */
@Slf4j
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER_USER_ID = "X-User-Id";
  private static final String HEADER_USER_ROLE = "X-User-Role";
  private static final String HEADER_USER_EMAIL = "X-User-Email";
  private static final String HEADER_USER_TYPE = "X-User-Type";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      String userId = request.getHeader(HEADER_USER_ID);

      if (StringUtils.hasText(userId)) {
        // 헤더에서 사용자 정보 추출
        String roleStr = request.getHeader(HEADER_USER_ROLE);
        String email = request.getHeader(HEADER_USER_EMAIL);
        String userType = request.getHeader(HEADER_USER_TYPE);

        UserRole role = parseRole(roleStr);

        // UserPrincipal 생성
        UserPrincipal principal = UserPrincipal.of(userId, email, role, userType);

        // Authentication 생성 및 SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("인증 설정 완료 - userId: {}, role: {}, userType: {}",
            userId, role, userType);
      }
    } catch (Exception e) {
      log.warn("헤더 인증 처리 중 오류 발생: {}", e.getMessage());
      // 인증 실패해도 필터 체인은 계속 진행 (permitAll 경로일 수 있음)
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  /**
   * 역할 문자열을 UserRole로 변환
   * <p>
   * 유효하지 않은 역할인 경우 기본값 USER 반환
   * </p>
   *
   * @param roleStr 역할 문자열
   * @return UserRole enum
   */
  private UserRole parseRole(String roleStr) {
    if (!StringUtils.hasText(roleStr)) {
      return UserRole.USER;
    }

    try {
      // "ROLE_" 접두사 제거
      if (roleStr.startsWith("ROLE_")) {
        roleStr = roleStr.substring(5);
      }
      return UserRole.valueOf(roleStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("유효하지 않은 역할: {}, 기본값 USER 사용", roleStr);
      return UserRole.USER;
    }
  }
}