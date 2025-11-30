package com.jun_bank.auth_server.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityContext에서 현재 사용자 정보 조회 유틸리티
 */
@Component
public class SecurityContextUtil {

    /**
     * 현재 인증된 사용자 ID 조회
     * @return 사용자 ID (인증 정보 없으면 null)
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String) {
            return (String) principal;
        }

        return authentication.getName();
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인
     * @return 인증 여부
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}