package com.jun_bank.auth_server.global.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

/**
 * 인증된 사용자 정보를 담는 Principal 객체
 * <p>
 * Gateway에서 JWT 검증 후 헤더로 전달된 사용자 정보를 담습니다.
 * Spring Security의 Authentication.getPrincipal()로 접근 가능합니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * @GetMapping("/me")
 * public UserResponse getMe(@AuthenticationPrincipal UserPrincipal principal) {
 *     String userId = principal.getUserId();
 *     // ...
 * }
 * }</pre>
 *
 * @param userId 사용자 ID (USR-xxxxxxxx)
 * @param role   사용자 역할 (USER, ADMIN)
 * @param email  사용자 이메일
 */
public record UserPrincipal(
        String userId,
        String role,
        String email
) implements Principal {

    /**
     * Principal 인터페이스 구현 - 사용자 식별자 반환
     *
     * @return userId
     */
    @Override
    public String getName() {
        return userId;
    }

    /**
     * Spring Security 권한 목록 반환
     * <p>
     * role 필드를 "ROLE_" 접두사가 붙은 GrantedAuthority로 변환합니다.
     *
     * @return 권한 목록
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * 관리자 여부 확인
     *
     * @return ADMIN 역할이면 true
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    /**
     * 일반 사용자 여부 확인
     *
     * @return USER 역할이면 true
     */
    public boolean isUser() {
        return "USER".equalsIgnoreCase(role);
    }
}