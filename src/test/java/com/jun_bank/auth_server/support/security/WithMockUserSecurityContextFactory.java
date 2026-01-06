package com.jun_bank.auth_server.support.security;

import com.jun_bank.auth_server.global.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * {@link MockUser} 애노테이션 기반으로 테스트 실행 시 SecurityContext를 생성하는 팩토리 클래스
 * <p>
 * Spring Security 테스트 프레임워크의 {@link WithSecurityContextFactory} 인터페이스를 구현하여,
 * 테스트 메서드 실행 전에 인증 정보를 SecurityContext에 설정합니다.
 * <p>
 * 주어진 {@link MockUser} 애노테이션의 속성(userId, role, email)을 기반으로
 * {@link UserPrincipal} 객체를 생성하고, 이를 principal로 가지는
 * {@link UsernamePasswordAuthenticationToken} 인증 객체를 구성합니다.
 */
public class WithMockUserSecurityContextFactory implements WithSecurityContextFactory<MockUser> {

    /**
     * 주어진 {@link MockUser} 정보를 바탕으로 SecurityContext를 생성합니다.
     *
     * @param mockUser 테스트용 사용자 정보가 포함된 애노테이션 인스턴스
     * @return 설정된 인증 정보가 포함된 SecurityContext
     */
    @Override
    public SecurityContext createSecurityContext(MockUser mockUser) {
        UserPrincipal userPrincipal = new UserPrincipal(
                mockUser.userId(),
                mockUser.role(),
                mockUser.email()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        return context;
    }
}