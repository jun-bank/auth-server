package com.jun_bank.auth_server.domain.auth.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 사용자 역할(권한)
 * <p>
 * 사용자의 권한 수준을 정의합니다.
 * Spring Security와 연동하여 접근 제어에 사용됩니다.
 *
 * <h3>권한 계층:</h3>
 * <pre>
 * ADMIN (level: 100)
 *   │
 *   └── USER (level: 1)
 *
 * ADMIN은 USER의 모든 권한을 포함합니다.
 * </pre>
 *
 * <h3>Spring Security 연동:</h3>
 * <pre>{@code
 * // 역할 기반 접근 제어
 * @PreAuthorize("hasRole('ADMIN')")
 * public void adminOnly() { ... }
 *
 * // 권한 문자열 생성
 * String authority = role.toAuthority();  // "ROLE_ADMIN"
 * }</pre>
 *
 * @see org.springframework.security.core.GrantedAuthority
 */
public enum UserRole {

    /**
     * 일반 사용자
     * <p>
     * 기본 서비스 이용이 가능한 역할입니다.
     * 회원가입 시 기본으로 부여됩니다.
     * </p>
     */
    USER("일반 사용자", 1),

    /**
     * 관리자
     * <p>
     * 시스템 관리 기능을 포함하는 역할입니다.
     * 사용자 관리, 시스템 설정 등의 권한을 가집니다.
     * </p>
     */
    ADMIN("관리자", 100);

    private final String description;
    private final int level;

    /**
     * UserRole 생성자
     *
     * @param description 역할 설명
     * @param level 권한 레벨 (높을수록 높은 권한)
     */
    UserRole(String description, int level) {
        this.description = description;
        this.level = level;
    }

    /**
     * 역할 설명 반환
     *
     * @return 한글 역할 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 권한 레벨 반환
     * <p>
     * 숫자가 높을수록 높은 권한을 의미합니다.
     * 권한 계층 비교에 사용됩니다.
     * </p>
     *
     * @return 권한 레벨 (USER: 1, ADMIN: 100)
     */
    public int getLevel() {
        return level;
    }

    /**
     * 관리자 여부 확인
     *
     * @return ADMIN이면 true
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 일반 사용자 여부 확인
     *
     * @return USER이면 true
     */
    public boolean isUser() {
        return this == USER;
    }

    /**
     * 특정 역할의 권한을 포함하는지 확인
     * <p>
     * 권한 계층에 따라 상위 역할은 하위 역할의 권한을 포함합니다.
     * </p>
     *
     * <h4>예시:</h4>
     * <pre>{@code
     * ADMIN.hasAuthority(USER);   // true - ADMIN은 USER 권한 포함
     * USER.hasAuthority(ADMIN);   // false - USER는 ADMIN 권한 미포함
     * USER.hasAuthority(USER);    // true - 자기 자신의 권한은 포함
     * }</pre>
     *
     * @param role 확인할 역할
     * @return 해당 역할의 권한을 포함하면 true
     */
    public boolean hasAuthority(UserRole role) {
        return this.level >= role.level;
    }

    /**
     * 현재 역할이 가진 모든 권한(역할) 반환
     * <p>
     * 권한 계층에 따라 현재 역할 이하의 모든 역할을 반환합니다.
     * </p>
     *
     * <h4>예시:</h4>
     * <pre>{@code
     * ADMIN.getIncludedRoles();  // {USER, ADMIN}
     * USER.getIncludedRoles();   // {USER}
     * }</pre>
     *
     * @return 포함된 역할 Set
     */
    public Set<UserRole> getIncludedRoles() {
        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
        for (UserRole role : UserRole.values()) {
            if (this.hasAuthority(role)) {
                roles.add(role);
            }
        }
        return roles;
    }

    /**
     * Spring Security Authority 문자열 반환
     * <p>
     * Spring Security에서 사용하는 "ROLE_" 접두사가 붙은 역할명을 반환합니다.
     * {@link org.springframework.security.core.authority.SimpleGrantedAuthority}
     * 생성에 사용됩니다.
     * </p>
     *
     * @return "ROLE_" 접두사가 붙은 역할명 (예: "ROLE_ADMIN")
     */
    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}