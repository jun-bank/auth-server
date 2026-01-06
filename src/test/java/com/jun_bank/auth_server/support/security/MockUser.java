package com.jun_bank.auth_server.support.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 테스트 환경에서 Spring Security의 인증 정보를 설정하기 위한 커스텀 애노테이션
 * <p>
 * Spring Security의 {@link WithSecurityContext} 메커니즘을 활용하여,
 * 테스트 실행 시 지정된 사용자 정보로 SecurityContext를 구성합니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 기본 사용자 (USER)
 * @Test
 * @MockUser(userId = "USR-12345678")
 * void testUserAccess() {
 *     // 테스트 코드
 * }
 *
 * // 관리자 권한으로 테스트
 * @Test
 * @MockUser(userId = "USR-admin001", role = "ADMIN")
 * void testAdminAccess() {
 *     // 테스트 코드
 * }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserSecurityContextFactory.class)
public @interface MockUser {

    /**
     * 테스트 사용자 ID
     * <p>기본값은 "USR-test1234"
     */
    String userId() default "USR-test1234";

    /**
     * 테스트 사용자 역할
     * <p>기본값은 "USER"
     * <p>설정된 역할에 따라 ROLE_USER, ROLE_ADMIN 등의 권한이 부여됩니다.
     */
    String role() default "USER";

    /**
     * 테스트 사용자 이메일
     * <p>기본값은 "test@example.com"
     */
    String email() default "test@example.com";
}