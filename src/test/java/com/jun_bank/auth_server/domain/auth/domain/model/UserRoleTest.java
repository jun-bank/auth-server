package com.jun_bank.auth_server.domain.auth.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserRole 테스트")
class UserRoleTest {

    @Test
    @DisplayName("USER 역할 특성")
    void user_Characteristics() {
        UserRole role = UserRole.USER;

        assertThat(role.isUser()).isTrue();
        assertThat(role.isAdmin()).isFalse();
        assertThat(role.getLevel()).isEqualTo(1);
        assertThat(role.getDescription()).isEqualTo("일반 사용자");
    }

    @Test
    @DisplayName("ADMIN 역할 특성")
    void admin_Characteristics() {
        UserRole role = UserRole.ADMIN;

        assertThat(role.isAdmin()).isTrue();
        assertThat(role.isUser()).isFalse();
        assertThat(role.getLevel()).isEqualTo(100);
        assertThat(role.getDescription()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("ADMIN은 USER 권한 포함")
    void admin_HasUserAuthority() {
        assertThat(UserRole.ADMIN.hasAuthority(UserRole.USER)).isTrue();
        assertThat(UserRole.ADMIN.hasAuthority(UserRole.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("USER는 ADMIN 권한 미포함")
    void user_DoesNotHaveAdminAuthority() {
        assertThat(UserRole.USER.hasAuthority(UserRole.ADMIN)).isFalse();
        assertThat(UserRole.USER.hasAuthority(UserRole.USER)).isTrue();
    }

    @Test
    @DisplayName("Spring Security Authority 문자열")
    void toAuthority() {
        assertThat(UserRole.ADMIN.toAuthority()).isEqualTo("ROLE_ADMIN");
        assertThat(UserRole.USER.toAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("getIncludedRoles - ADMIN")
    void getIncludedRoles_Admin() {
        assertThat(UserRole.ADMIN.getIncludedRoles())
                .containsExactlyInAnyOrder(UserRole.USER, UserRole.ADMIN);
    }

    @Test
    @DisplayName("getIncludedRoles - USER")
    void getIncludedRoles_User() {
        assertThat(UserRole.USER.getIncludedRoles())
                .containsExactly(UserRole.USER);
    }
}